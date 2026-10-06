package com.findu.transaction.dailybalance.service;

import com.findu.transaction.dailybalance.entity.DailyBalanceEntity;
import com.findu.transaction.dailybalance.entity.ExtraExpenseRequestEntity;
import com.findu.transaction.dailybalance.entity.ProviderAccountEntity;
import com.findu.transaction.dailybalance.entity.ServiceTransactionEntity;
import com.findu.transaction.dailybalance.model.DailyBalanceCalculation;
import com.findu.transaction.dailybalance.model.DailyBalanceExecutionResult;
import com.findu.transaction.dailybalance.repository.DailyBalanceJpaRepository;
import com.findu.transaction.dailybalance.repository.ExtraExpenseQueryRepository;
import com.findu.transaction.dailybalance.repository.ProviderAccountQueryRepository;
import com.findu.transaction.dailybalance.repository.ServiceTransactionQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBalanceProcessor {

    private final DailyBalanceJpaRepository dailyBalanceJpaRepository;
    private final ServiceTransactionQueryRepository transactionQueryRepository;
    private final ExtraExpenseQueryRepository extraExpenseQueryRepository;
    private final ProviderAccountQueryRepository providerAccountQueryRepository;

    @Value("${findu.daily-balance.timezone:America/Bogota}")
    private String timezoneStr;

    public DailyBalanceExecutionResult executeFinancialCut(LocalDate date, Long optionalProviderId) {
        String executionId = UUID.randomUUID().toString();
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of(timezoneStr));
        Instant startedAt = Instant.now();

        log.info("DailyBalanceProcessor: Iniciando corte financiero diario [executionId={}, balanceDate={}, providerId={}]",
                executionId, targetDate, optionalProviderId);

        Set<Long> providerIdsToProcess = new LinkedHashSet<>();
        if (optionalProviderId != null) {
            providerIdsToProcess.add(optionalProviderId);
        } else {
            // Obtener proveedores registrados en la BD
            List<ProviderAccountEntity> accounts = providerAccountQueryRepository.findAll();
            for (ProviderAccountEntity acc : accounts) {
                providerIdsToProcess.add(acc.getProviderId());
            }

            // También incluir proveedores que hayan tenido transacciones en la fecha
            ZoneId zoneId = ZoneId.of(timezoneStr);
            Instant start = targetDate.atStartOfDay(zoneId).toInstant();
            Instant end = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            List<Long> activeProviders = transactionQueryRepository.findActiveProviderIdsBetween(start, end);
            providerIdsToProcess.addAll(activeProviders);
        }

        int processed = 0;
        int succeeded = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long providerId : providerIdsToProcess) {
            processed++;
            try {
                processSingleProviderCut(providerId, targetDate);
                succeeded++;
            } catch (Exception e) {
                failed++;
                String errorMsg = String.format("Error procesando providerId=%d: %s", providerId, e.getMessage());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        Instant finishedAt = Instant.now();
        String finalStatus = failed == 0 ? "COMPLETED" : (succeeded > 0 ? "PARTIAL_SUCCESS" : "FAILED");

        log.info("DailyBalanceProcessor: Corte diario finalizado [executionId={}, processed={}, succeeded={}, failed={}, status={}]",
                executionId, processed, succeeded, failed, finalStatus);

        return DailyBalanceExecutionResult.builder()
                .executionId(executionId)
                .balanceDate(targetDate)
                .providersProcessed(processed)
                .providersSucceeded(succeeded)
                .providersFailed(failed)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .status(finalStatus)
                .errors(errors)
                .build();
    }

    @Transactional
    public DailyBalanceEntity processSingleProviderCut(Long providerId, LocalDate balanceDate) {
        ZoneId zoneId = ZoneId.of(timezoneStr);
        Instant start = balanceDate.atStartOfDay(zoneId).toInstant();
        Instant end = balanceDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        // 1. Obtener saldo inicial (opening balance) del día anterior
        LocalDate previousDate = balanceDate.minusDays(1);
        BigDecimal openingBalance = dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, previousDate)
                .map(DailyBalanceEntity::getClosingBalance)
                .orElse(BigDecimal.ZERO);

        // 2. Consultar transacciones de servicio del día
        List<ServiceTransactionEntity> transactions = transactionQueryRepository
                .findByProviderIdAndCreatedAtBetween(providerId, start, end);

        BigDecimal totalEarnings = BigDecimal.ZERO;
        BigDecimal totalCommissions = BigDecimal.ZERO;
        BigDecimal totalTips = BigDecimal.ZERO;
        BigDecimal cashPayments = BigDecimal.ZERO;
        BigDecimal digitalPayments = BigDecimal.ZERO;
        BigDecimal cashCommissions = BigDecimal.ZERO;
        BigDecimal digitalEarnings = BigDecimal.ZERO;

        for (ServiceTransactionEntity tx : transactions) {
            BigDecimal serviceAmt = tx.getServiceAmount() != null ? tx.getServiceAmount() : BigDecimal.ZERO;
            BigDecimal commAmt = tx.getCommissionAmount() != null ? tx.getCommissionAmount() : BigDecimal.ZERO;
            BigDecimal tipAmt = tx.getTipAmount() != null ? tx.getTipAmount() : BigDecimal.ZERO;
            BigDecimal provAmt = tx.getProviderAmount() != null ? tx.getProviderAmount() : BigDecimal.ZERO;
            BigDecimal totalCustAmt = tx.getTotalCustomerAmount() != null ? tx.getTotalCustomerAmount() : BigDecimal.ZERO;

            totalEarnings = totalEarnings.add(provAmt);
            totalCommissions = totalCommissions.add(commAmt);
            totalTips = totalTips.add(tipAmt);

            if ("CASH".equalsIgnoreCase(tx.getPaymentMethod())) {
                cashPayments = cashPayments.add(totalCustAmt);
                cashCommissions = cashCommissions.add(commAmt);
            } else {
                digitalPayments = digitalPayments.add(totalCustAmt);
                digitalEarnings = digitalEarnings.add(provAmt);
            }
        }

        // 3. Consultar gastos extra aprobados
        List<ExtraExpenseRequestEntity> approvedExtras = extraExpenseQueryRepository
                .findApprovedByProviderIdAndApprovedAtBetween(providerId, start, end);

        BigDecimal totalExtras = BigDecimal.ZERO;
        for (ExtraExpenseRequestEntity extra : approvedExtras) {
            if (extra.getAmount() != null) {
                totalExtras = totalExtras.add(extra.getAmount());
            }
        }

        // 4. Calcular deuda acumulada y saldo a favor pendiente
        BigDecimal providerDebt = cashCommissions;
        BigDecimal pendingPayout = digitalEarnings.add(totalTips).add(totalExtras);
        BigDecimal closingBalance = openingBalance.add(totalEarnings).add(totalTips).add(totalExtras).subtract(totalCommissions);

        DailyBalanceCalculation calc = DailyBalanceCalculation.builder()
                .providerId(providerId)
                .balanceDate(balanceDate)
                .openingBalance(openingBalance)
                .totalEarnings(totalEarnings)
                .totalCommissions(totalCommissions)
                .totalTips(totalTips)
                .totalExtras(totalExtras)
                .cashPayments(cashPayments)
                .digitalPayments(digitalPayments)
                .providerDebt(providerDebt)
                .pendingPayout(pendingPayout)
                .closingBalance(closingBalance)
                .currency("COP")
                .build();

        // 5. Persistir o actualizar respetando idempotencia
        return saveOrUpdateDailyBalance(calc);
    }

    private DailyBalanceEntity saveOrUpdateDailyBalance(DailyBalanceCalculation calc) {
        Optional<DailyBalanceEntity> existingOpt = dailyBalanceJpaRepository
                .findByProviderIdAndBalanceDate(calc.getProviderId(), calc.getBalanceDate());

        DailyBalanceEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setUpdatedAt(Instant.now());
            log.info("DailyBalanceProcessor: Actualizando balance existente para providerId={} y fecha={}",
                    calc.getProviderId(), calc.getBalanceDate());
        } else {
            entity = new DailyBalanceEntity();
            entity.setId("BAL-" + calc.getProviderId() + "-" + calc.getBalanceDate());
            entity.setProviderId(calc.getProviderId());
            entity.setBalanceDate(calc.getBalanceDate());
            entity.setCreatedAt(Instant.now());
            log.info("DailyBalanceProcessor: Registrando nuevo balance para providerId={} y fecha={}",
                    calc.getProviderId(), calc.getBalanceDate());
        }

        entity.setOpeningBalance(calc.getOpeningBalance());
        entity.setTotalEarnings(calc.getTotalEarnings());
        entity.setTotalCommissions(calc.getTotalCommissions());
        entity.setTotalTips(calc.getTotalTips());
        entity.setTotalExtras(calc.getTotalExtras());
        entity.setCashPayments(calc.getCashPayments());
        entity.setDigitalPayments(calc.getDigitalPayments());
        entity.setProviderDebt(calc.getProviderDebt());
        entity.setPendingPayout(calc.getPendingPayout());
        entity.setClosingBalance(calc.getClosingBalance());
        entity.setCurrency(calc.getCurrency());
        entity.setStatus("CALCULATED");

        return dailyBalanceJpaRepository.save(entity);
    }
}

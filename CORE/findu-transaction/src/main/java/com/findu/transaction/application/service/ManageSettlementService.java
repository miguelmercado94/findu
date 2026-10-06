package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.CreateSettlementUseCase;
import com.findu.transaction.application.port.in.GetSettlementUseCase;
import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.application.port.in.query.GetSettlementQuery;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ProviderSettlementProfileRepository;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.event.SettlementCreatedEvent;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementExecutionResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageSettlementService implements CreateSettlementUseCase, GetSettlementUseCase {

    private final SettlementRepository settlementRepository;
    private final ProviderSettlementProfileRepository profileRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;

    private static final String DEFAULT_TIMEZONE = "America/Bogota";

    @Override
    @Transactional
    public Settlement execute(CreateSettlementCommand command) {
        if (command == null || command.getProviderId() == null) {
            throw new IllegalArgumentException("El comando de settlement y el providerId son obligatorios");
        }

        Long providerId = command.getProviderId();
        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));

        ProviderSettlementProfile profile = profileRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    ProviderSettlementProfile defaultProf = ProviderSettlementProfile.createDefault(providerId, account.getProviderType());
                    return profileRepository.save(defaultProf);
                });

        SettlementType settlementType = command.getSettlementType() != null ?
                command.getSettlementType() : profile.getSettlementFrequency();

        LocalDate refDate = command.getTargetDate() != null ? command.getTargetDate() :
                (command.getStartDate() != null ? command.getStartDate() : LocalDate.now(ZoneId.of(DEFAULT_TIMEZONE)));

        LocalDate startDate;
        LocalDate endDate;

        if (command.getStartDate() != null && command.getEndDate() != null) {
            startDate = command.getStartDate();
            endDate = command.getEndDate();
        } else if (settlementType == SettlementType.WEEKLY) {
            startDate = refDate.with(DayOfWeek.MONDAY);
            endDate = refDate.with(DayOfWeek.SUNDAY);
        } else {
            startDate = refDate;
            endDate = refDate;
        }

        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        Instant periodStart = startDate.atStartOfDay(zoneId).toInstant();
        Instant periodEnd = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        // 1. Idempotencia: Verificar si el Settlement ya fue generado para este período y tipo
        Optional<Settlement> existing = settlementRepository.findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(
                providerId, periodStart, periodEnd, settlementType);
        if (existing.isPresent()) {
            log.info("Settlement ya existente (Idempotente): providerId={}, periodStart={}, periodEnd={}, settlementId={}",
                    providerId, periodStart, periodEnd, existing.get().getId());
            return existing.get();
        }

        // 2. Obtener DailyBalances elegibles en el rango de fechas
        List<DailyBalance> dailyBalances = dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(
                providerId, startDate, endDate);

        List<String> alreadySettledIds = settlementRepository.findSettledDailyBalanceIdsByProviderId(providerId);

        List<DailyBalance> eligibleBalances = dailyBalances.stream()
                .filter(b -> b.getId() != null && !alreadySettledIds.contains(b.getId()))
                .collect(Collectors.toList());

        if (eligibleBalances.isEmpty()) {
            log.info("No se encontraron DailyBalances elegibles para liquidar: providerId={}, startDate={}, endDate={}",
                    providerId, startDate, endDate);
            Settlement skipped = Settlement.create(
                    providerId,
                    account.getProviderType(),
                    settlementType,
                    Money.ZERO,
                    Money.ZERO,
                    periodStart,
                    periodEnd,
                    Collections.emptyList()
            );
            skipped.markAsSkipped();
            return settlementRepository.save(skipped);
        }

        // 3. Calcular montos acumulados de la liquidación
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDebtComp = BigDecimal.ZERO;
        List<String> eligibleIds = new ArrayList<>();

        for (DailyBalance db : eligibleBalances) {
            eligibleIds.add(db.getId());
            BigDecimal pending = db.getPendingPayout() != null ? db.getPendingPayout().getAmount() : BigDecimal.ZERO;
            BigDecimal debt = db.getProviderDebt() != null ? db.getProviderDebt().getAmount() : BigDecimal.ZERO;

            totalGross = totalGross.add(pending);
            totalDebtComp = totalDebtComp.add(debt);
        }

        Money grossMoney = Money.of(totalGross);
        Money compensationMoney = Money.of(totalDebtComp);

        Settlement settlement = Settlement.create(
                providerId,
                account.getProviderType(),
                settlementType,
                grossMoney,
                compensationMoney,
                periodStart,
                periodEnd,
                eligibleIds
        );

        // Check restricción financiera por saldo negativo acumulado
        Money currentPayable = account.getPayableBalance();
        Money currentReceivable = account.getReceivableBalance();
        Money netReceivable = currentReceivable.subtract(currentPayable);
        if (netReceivable.isGreaterThan(profile.getNegativeBalanceLimit())) {
            settlement.markAsRestricted();
            log.warn("Provider id={} supera límite de saldo negativo limit={}, netDebt={}",
                    providerId, profile.getNegativeBalanceLimit().getAmount(), netReceivable.getAmount());
        }

        Settlement saved = settlementRepository.save(settlement);

        // 4. Publicar evento SettlementCreatedEvent
        SettlementCreatedEvent createdEvent = new SettlementCreatedEvent(
                saved.getId(),
                saved.getProviderId(),
                saved.getPeriodStart(),
                saved.getPeriodEnd(),
                saved.getNetAmount()
        );

        if (eventPublisher != null) {
            eventPublisher.publish(createdEvent);
        }
        if (transactionEventPublisher != null) {
            transactionEventPublisher.publish(createdEvent.toEnvelope(null));
        }

        log.info("Settlement creado exitosamente: id={}, code={}, providerId={}, netAmount={}, status={}",
                saved.getId(), saved.getSettlementCode(), saved.getProviderId(), saved.getNetAmount().getAmount(), saved.getStatus());

        return saved;
    }

    @Transactional
    public SettlementExecutionResultResponse runBatchSettlements(LocalDate targetDate, String settlementTypeFilter) {
        String executionId = "exec-set-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = targetDate != null ? targetDate : LocalDate.now(ZoneId.of(DEFAULT_TIMEZONE));
        Instant startedAt = Instant.now();

        List<Long> providerIds = providerAccountRepository.findAll().stream()
                .map(ProviderAccount::getProviderId)
                .distinct()
                .collect(Collectors.toList());
        if (providerIds.isEmpty()) {
            providerIds = List.of(100L); // fallback for dev
        }

        int processed = 0;
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        SettlementType filterType = null;
        if (settlementTypeFilter != null) {
            try {
                filterType = SettlementType.valueOf(settlementTypeFilter.toUpperCase());
            } catch (Exception ignored) {}
        }

        for (Long providerId : providerIds) {
            processed++;
            try {
                CreateSettlementCommand cmd = CreateSettlementCommand.builder()
                        .providerId(providerId)
                        .targetDate(date)
                        .settlementType(filterType)
                        .build();

                Settlement result = execute(cmd);
                if (result.getStatus() == SettlementStatus.SKIPPED) {
                    skipped++;
                } else {
                    created++;
                }
            } catch (Exception e) {
                failed++;
                String err = String.format("Error liquidando providerId=%d: %s", providerId, e.getMessage());
                log.error(err, e);
                errors.add(err);
            }
        }

        Instant finishedAt = Instant.now();
        String status = failed == 0 ? "COMPLETED" : (created > 0 ? "PARTIAL_SUCCESS" : "FAILED");

        return SettlementExecutionResultResponse.builder()
                .executionId(executionId)
                .targetDate(date)
                .providersProcessed(processed)
                .settlementsCreated(created)
                .settlementsSkipped(skipped)
                .providersFailed(failed)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .status(status)
                .errors(errors)
                .build();
    }

    @Override
    public Optional<Settlement> execute(GetSettlementQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        if (query.getSettlementId() != null) {
            return settlementRepository.findById(query.getSettlementId());
        }
        if (query.getProviderId() != null) {
            List<Settlement> list = settlementRepository.findByProviderId(query.getProviderId());
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
        }
        return Optional.empty();
    }
}

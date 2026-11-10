package com.findu.transaction.application.service.finance;

import com.findu.transaction.application.port.in.finance.dto.PendingBalanceView;
import com.findu.transaction.application.port.out.persistence.PayoutRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.TransactionStatus;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.service.SettlementBalanceCalculator;
import com.findu.transaction.domain.service.SettlementBalanceCalculator.PendingAmounts;
import com.findu.transaction.domain.service.SettlementSchedule;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * FUENTE UNICA del saldo pendiente de liquidar. Lo usan el resumen semanal, el historial de movimientos
 * y la ejecucion de la liquidacion, de modo que todas las pantallas muestran el mismo neto.
 *
 * <p>La verdad es {@link ProviderAccount} (payable/receivable) + ledger por fecha. NO se suma
 * {@code daily_balances.pending_payout}: es un snapshot acumulado y duplicaria montos.
 *
 * <p>Ventana de una liquidacion = (periodEnd de la ultima liquidacion COMPLETED, corte]. El bruto de una
 * ventana es min(payable, suma de SERVICE_EARNING CREDIT en la ventana). El neto aplica
 * {@link SettlementBalanceCalculator}.
 */
@Service
@RequiredArgsConstructor
public class PendingBalanceService {

    private static final ZoneId ZONE = SettlementSchedule.BOGOTA;

    private final ProviderAccountRepository providerAccountRepository;
    private final SettlementRepository settlementRepository;
    private final PayoutRepository payoutRepository;
    private final ServiceTransactionRepository serviceTransactionRepository;

    @Transactional(readOnly = true)
    public PendingBalanceView getPendingBalance(Long providerId, LocalDate today) {
        ProviderAccount account = providerAccountRepository.findByProviderId(providerId)
                .orElseGet(() -> ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL));
        Optional<Settlement> last = settlementRepository.findLatestCompletedByProviderId(providerId);
        Instant windowStart = last.map(Settlement::getPeriodEnd).orElse(Instant.EPOCH);
        BigDecimal debt = account.getReceivableBalance().getAmount();

        // Saldo total pendiente (sin tope superior): todo el payable.
        PendingAmounts total = SettlementBalanceCalculator.calculate(account.getPayableBalance().getAmount(), debt);

        // Arrastre: lo ganado antes del lunes de la semana actual.
        Instant mondayStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZONE).toInstant();
        BigDecimal carriedGross = settlableGross(account, windowStart, mondayStart);
        BigDecimal thisWeekGross = total.gross().subtract(carriedGross).max(BigDecimal.ZERO);

        // Proxima liquidacion: proximo martes; si lo ganado no entra en su corte, se salta al siguiente.
        LocalDate settlementDate = SettlementSchedule.nextTuesdayAfter(today);
        BigDecimal grossAtCutoff = grossAtCutoff(account, windowStart, settlementDate);
        if (grossAtCutoff.signum() == 0 && total.gross().signum() > 0) {
            settlementDate = settlementDate.plusDays(7);
            grossAtCutoff = grossAtCutoff(account, windowStart, settlementDate);
        }
        PendingAmounts next = SettlementBalanceCalculator.calculate(grossAtCutoff, debt);

        int pendingServices = (int) serviceTransactionRepository.findByProviderId(providerId).stream()
                .filter(tx -> tx.getCreatedAt() != null && !tx.getCreatedAt().isBefore(windowStart))
                .filter(tx -> tx.getTransactionStatus() != TransactionStatus.CANCELLED)
                .count();

        return PendingBalanceView.builder()
                .grossAmount(Money.of(total.gross()))
                .debtCompensationAmount(Money.of(total.debtCompensation()))
                .netAmount(Money.of(total.net()))
                .remainingDebt(Money.of(total.remainingDebt()))
                .thisWeekGrossAmount(Money.of(thisWeekGross))
                .carriedOverGrossAmount(Money.of(carriedGross))
                .carriedOver(carriedGross.signum() > 0)
                .pendingServicesCount(pendingServices)
                .nextSettlementDate(settlementDate)
                .nextSettlementCutoff(SettlementSchedule.cutoffDate(settlementDate))
                .nextSettlementNet(Money.of(next.net()))
                .lastSettlement(last.map(this::toLastSettlement).orElse(null))
                .build();
    }

    /**
     * Bruto liquidable de una ventana [windowStart, windowEndExclusive): min(payable, ganancias en la ventana).
     * {@code windowEndExclusive == null} = sin tope superior.
     */
    public BigDecimal settlableGross(ProviderAccount account, Instant windowStart, Instant windowEndExclusive) {
        BigDecimal earned = account.getLedgerEntries().stream()
                .filter(e -> e.getType() == LedgerEntryType.SERVICE_EARNING && e.getDirection() == LedgerDirection.CREDIT)
                .filter(e -> e.getCreatedAt() != null)
                .filter(e -> windowStart == null || !e.getCreatedAt().isBefore(windowStart))
                .filter(e -> windowEndExclusive == null || e.getCreatedAt().isBefore(windowEndExclusive))
                .map(e -> e.getAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return earned.min(account.getPayableBalance().getAmount()).max(BigDecimal.ZERO);
    }

    private BigDecimal grossAtCutoff(ProviderAccount account, Instant windowStart, LocalDate settlementDate) {
        LocalDate cutoff = SettlementSchedule.cutoffDate(settlementDate);
        return settlableGross(account, windowStart, SettlementSchedule.periodEndExclusive(cutoff, ZONE));
    }

    private PendingBalanceView.LastSettlementView toLastSettlement(Settlement s) {
        String reference = payoutRepository.findBySettlementId(s.getId())
                .map(Payout::getExternalReference).orElse(null);
        return PendingBalanceView.LastSettlementView.builder()
                .settlementCode(s.getSettlementCode())
                .status(s.getStatus().name())
                .grossAmount(s.getGrossAmount())
                .debtCompensationAmount(s.getDebtCompensationAmount())
                .netAmount(s.getNetAmount())
                .executedAt(s.getExecutedAt())
                .reference(reference)
                .build();
    }
}

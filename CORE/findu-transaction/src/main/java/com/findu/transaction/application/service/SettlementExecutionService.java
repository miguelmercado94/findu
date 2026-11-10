package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ExecuteSettlementUseCase;
import com.findu.transaction.application.port.in.dto.SettlementRunResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult.Outcome;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort.PayoutTransferRequest;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort.PayoutTransferResult;
import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.application.port.out.persistence.PayoutRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.application.service.finance.PendingBalanceService;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.event.PayoutCompletedEvent;
import com.findu.transaction.domain.event.SettlementCreatedEvent;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.service.SettlementBalanceCalculator;
import com.findu.transaction.domain.service.SettlementBalanceCalculator.PendingAmounts;
import com.findu.transaction.domain.service.SettlementSchedule;
import com.findu.transaction.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Liquidacion (simulada) de un proveedor: idempotente y transaccional.
 *
 * <h3>Reglas</h3>
 * <ul>
 *   <li>Corte = domingo estrictamente anterior a la fecha de ejecucion; liquida el martes
 *       (ver {@link SettlementSchedule}). Lo ganado despues del corte se arrastra a la proxima liquidacion.</li>
 *   <li>Bruto = ganancias de la ventana (periodEnd de la ultima liquidacion COMPLETED, corte]; neto =
 *       bruto - compensacion de deuda de comision en efectivo ({@link SettlementBalanceCalculator}).</li>
 *   <li>Si neto &lt;= 0 (deuda &gt;= bruto) no se transfiere nada: se compensa lo posible, la deuda restante
 *       queda a favor de la plataforma y el resultado es NET_ZERO_NO_TRANSFER.</li>
 *   <li>Idempotencia: codigo determinista {@code SET-{providerId}-{corte}} (UNIQUE) y ventana vacia
 *       => no se crea nada. Dos ejecuciones nunca pagan dos veces; {@code payouts.settlement_id} es UNIQUE.</li>
 *   <li>Si la pasarela falla se lanza excepcion y TODO hace rollback (sin estados parciales).</li>
 * </ul>
 * La deuda compensada se refleja en el ledger (COMPENSATION_ADJUSTMENT) y en receivable_balance; el pago
 * al proveedor como asiento PAYOUT y registro en {@code payouts}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementExecutionService implements ExecuteSettlementUseCase {

    private static final ZoneId ZONE = SettlementSchedule.BOGOTA;

    private final ProviderAccountRepository providerAccountRepository;
    private final SettlementRepository settlementRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final PayoutRepository payoutRepository;
    private final PendingBalanceService pendingBalanceService;
    private final ObjectProvider<PayoutGatewayPort> payoutGatewayProvider;
    private final EventPublisherPort eventPublisher;
    private final TransactionEventPublisher transactionEventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public SettlementRunResult executeForProvider(Long providerId, LocalDate cutoffDate) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId es obligatorio");
        }
        Instant now = clock.instant();
        LocalDate cutoff = cutoffDate != null ? cutoffDate : SettlementSchedule.cutoffDate(LocalDate.now(clock));
        String code = SettlementSchedule.settlementCode(providerId, cutoff);

        Optional<Settlement> existing = settlementRepository.findBySettlementCode(code);
        if (existing.isPresent()) {
            return fromExisting(existing.get(), Outcome.ALREADY_SETTLED);
        }

        Optional<ProviderAccount> accountOpt = providerAccountRepository.findByProviderId(providerId);
        if (accountOpt.isEmpty()) {
            return nothing(providerId, code, "El proveedor no tiene cuenta financiera");
        }
        ProviderAccount account = accountOpt.get();

        Instant periodEnd = SettlementSchedule.periodEndExclusive(cutoff, ZONE);
        Optional<Settlement> last = settlementRepository.findLatestCompletedByProviderId(providerId);
        Instant windowStart = last.map(Settlement::getPeriodEnd).orElse(Instant.EPOCH);
        if (!periodEnd.isAfter(windowStart)) {
            return nothing(providerId, code, "El corte " + cutoff + " ya esta cubierto por una liquidacion anterior");
        }

        BigDecimal gross = pendingBalanceService.settlableGross(account, windowStart, periodEnd);
        if (gross.signum() <= 0) {
            return nothing(providerId, code, "Sin saldo para liquidar hasta el corte " + cutoff);
        }

        PendingAmounts calc = SettlementBalanceCalculator.calculate(gross, account.getReceivableBalance().getAmount());
        boolean transfer = calc.net().signum() > 0;
        PayoutGatewayPort gateway = null;
        if (transfer) {
            gateway = payoutGatewayProvider.getIfAvailable();
            if (gateway == null) {
                throw new IllegalStateException("Pasarela de payouts no configurada (findu.settlement.mock.enabled=false y sin adaptador real)");
            }
        }

        // Cierres diarios no liquidados hasta el corte (solo se marcan; el monto lo da el ledger).
        List<DailyBalance> toSettle = dailyBalanceRepository.findByProviderId(providerId).stream()
                .filter(b -> !b.isSettled() && b.getBalanceDate() != null && !b.getBalanceDate().isAfter(cutoff))
                .collect(Collectors.toList());

        Instant periodStart = last.isPresent() ? windowStart : earliestEarning(account, periodEnd);
        Settlement settlement = Settlement.createExecuted(code, providerId, account.getProviderType(), SettlementType.WEEKLY,
                Money.of(calc.gross()), Money.of(calc.debtCompensation()), periodStart, periodEnd,
                LocalDate.now(clock).atStartOfDay(ZONE).toInstant(),
                toSettle.stream().map(DailyBalance::getId).collect(Collectors.toList()));

        // Compensacion de la deuda de comision en efectivo contra lo ganado.
        if (calc.debtCompensation().signum() > 0) {
            account.applyCompensation(Money.of(calc.debtCompensation()));
        }

        // Transferencia (mock hoy) y registro del payout.
        Payout payout = null;
        if (transfer) {
            payout = Payout.create(settlement.getId(), providerId, Money.of(calc.net()));
            PayoutTransferResult result = gateway.transfer(new PayoutTransferRequest(
                    payout.getId(), providerId, payout.getAmount(), code));
            if (result == null || !result.successful()) {
                throw new IllegalStateException("La pasarela rechazo la transferencia: "
                        + (result != null ? result.message() : "sin respuesta"));
            }
            payout.markCompleted(result.reference());
            account.recordPayout(payout.getAmount(), payout.getPayoutCode());
        }

        toSettle.forEach(b -> b.markSettled(settlement.getId(), now));
        settlement.complete(now);

        Settlement saved = settlementRepository.save(settlement);
        providerAccountRepository.save(account);
        if (payout != null) {
            payoutRepository.save(payout);
        }
        toSettle.forEach(dailyBalanceRepository::save);

        publishEvents(saved, payout, now);
        log.info("Liquidacion {} providerId={} bruto={} compensacion={} neto={} transferido={} ref={}",
                code, providerId, calc.gross(), calc.debtCompensation(), calc.net(), transfer,
                payout != null ? payout.getExternalReference() : null);

        return SettlementRunResult.builder()
                .providerId(providerId)
                .outcome(transfer ? Outcome.SETTLED : Outcome.NET_ZERO_NO_TRANSFER)
                .settlementId(saved.getId())
                .settlementCode(code)
                .grossAmount(calc.gross())
                .debtCompensationAmount(calc.debtCompensation())
                .netAmount(calc.net())
                .remainingDebt(calc.remainingDebt())
                .transferred(transfer)
                .payoutCode(payout != null ? payout.getPayoutCode() : null)
                .reference(payout != null ? payout.getExternalReference() : null)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .executedAt(now)
                .dailyBalancesSettled(toSettle.size())
                .message(transfer ? null : "Neto <= 0: no se transfiere; la deuda restante queda a favor de la plataforma")
                .build();
    }

    private Instant earliestEarning(ProviderAccount account, Instant fallback) {
        return account.getLedgerEntries().stream()
                .filter(e -> e.getType() == LedgerEntryType.SERVICE_EARNING && e.getCreatedAt() != null)
                .map(e -> e.getCreatedAt())
                .min(Instant::compareTo)
                .orElse(fallback);
    }

    private SettlementRunResult fromExisting(Settlement s, Outcome outcome) {
        String reference = payoutRepository.findBySettlementId(s.getId()).map(Payout::getExternalReference).orElse(null);
        return SettlementRunResult.builder()
                .providerId(s.getProviderId())
                .outcome(outcome)
                .settlementId(s.getId())
                .settlementCode(s.getSettlementCode())
                .grossAmount(s.getGrossAmount().getAmount())
                .debtCompensationAmount(s.getDebtCompensationAmount().getAmount())
                .netAmount(s.getNetAmount().getAmount())
                .transferred(reference != null)
                .reference(reference)
                .periodStart(s.getPeriodStart())
                .periodEnd(s.getPeriodEnd())
                .executedAt(s.getExecutedAt())
                .message("La liquidacion de este corte ya fue ejecutada")
                .build();
    }

    private SettlementRunResult nothing(Long providerId, String code, String message) {
        return SettlementRunResult.builder()
                .providerId(providerId)
                .outcome(Outcome.NOTHING_TO_SETTLE)
                .settlementCode(code)
                .grossAmount(BigDecimal.ZERO)
                .debtCompensationAmount(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .message(message)
                .build();
    }

    private void publishEvents(Settlement settlement, Payout payout, Instant now) {
        SettlementCreatedEvent created = new SettlementCreatedEvent(settlement.getId(), settlement.getProviderId(),
                settlement.getPeriodStart(), settlement.getPeriodEnd(), settlement.getNetAmount());
        if (eventPublisher != null) {
            eventPublisher.publish(created);
        }
        if (transactionEventPublisher != null) {
            transactionEventPublisher.publish(created.toEnvelope(null));
        }
        if (payout != null) {
            PayoutCompletedEvent completed = new PayoutCompletedEvent(payout.getId(), settlement.getId(),
                    payout.getProviderId(), payout.getAmount(), payout.getExternalReference(), now);
            if (eventPublisher != null) {
                eventPublisher.publish(completed);
            }
            if (transactionEventPublisher != null) {
                transactionEventPublisher.publish(completed.toEnvelope(null));
            }
        }
    }
}

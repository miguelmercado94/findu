package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ExecuteSettlementUseCase;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult.Outcome;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Accounts;
import com.findu.transaction.application.service.InMemoryFinanceRepos.DailyBalances;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Payouts;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Settlements;
import com.findu.transaction.application.service.finance.PendingBalanceService;
import com.findu.transaction.domain.enums.LedgerDirection;
import com.findu.transaction.domain.enums.LedgerEntryType;
import com.findu.transaction.domain.enums.PayoutStatus;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.account.ProviderLedgerEntry;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.infrastructure.adapter.out.gateway.MockPayoutGatewayAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementExecutionServiceTest {

    private static final Long PROVIDER = 1L;
    private static final LocalDate CUTOFF = LocalDate.of(2026, 11, 8); // domingo anterior al martes 10-nov

    private Accounts accounts;
    private Settlements settlements;
    private Payouts payouts;
    private DailyBalances dailyBalances;
    private EventPublisherPort eventPublisher;
    private TransactionEventPublisher txPublisher;
    private PayoutGatewayPort gateway;
    private SettlementExecutionService service;
    private SettlementExecutionService serviceWithoutGateway;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        accounts = new Accounts();
        settlements = new Settlements();
        payouts = new Payouts();
        dailyBalances = new DailyBalances();
        eventPublisher = mock(EventPublisherPort.class);
        txPublisher = mock(TransactionEventPublisher.class);
        gateway = mock(PayoutGatewayPort.class);
        when(gateway.transfer(any())).thenAnswer(inv -> new MockPayoutGatewayAdapter().transfer(inv.getArgument(0)));

        ServiceTransactionRepository txRepo = mock(ServiceTransactionRepository.class);
        when(txRepo.findByProviderId(any())).thenReturn(List.of());
        PendingBalanceService pending = new PendingBalanceService(accounts, settlements, payouts, txRepo);
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:23:00Z"), ZoneId.of("America/Bogota"));

        ObjectProvider<PayoutGatewayPort> withGateway = mock(ObjectProvider.class);
        when(withGateway.getIfAvailable()).thenReturn(gateway);
        ObjectProvider<PayoutGatewayPort> noGateway = mock(ObjectProvider.class);
        when(noGateway.getIfAvailable()).thenReturn(null);

        service = new SettlementExecutionService(accounts, settlements, dailyBalances, payouts, pending,
                withGateway, eventPublisher, txPublisher, clock);
        serviceWithoutGateway = new SettlementExecutionService(accounts, settlements, dailyBalances, payouts, pending,
                noGateway, eventPublisher, txPublisher, clock);
    }

    @Test
    @DisplayName("Ejemplo real: bruto 186300, compensacion 1300, neto 185000, saldo posterior 0")
    void settlesExample() {
        accounts.save(InMemoryFinanceRepos.exampleAccount(PROVIDER));
        accounts.saves = 0;
        DailyBalance db = InMemoryFinanceRepos.dailyBalance(PROVIDER, LocalDate.of(2026, 10, 26));
        dailyBalances.save(db);

        SettlementRunResult r = service.executeForProvider(PROVIDER, null); // corte por defecto = domingo 8-nov

        assertThat(r.getOutcome()).isEqualTo(Outcome.SETTLED);
        assertThat(r.getSettlementCode()).isEqualTo("SET-1-20261108");
        assertThat(r.getGrossAmount()).isEqualByComparingTo("186300");
        assertThat(r.getDebtCompensationAmount()).isEqualByComparingTo("1300");
        assertThat(r.getNetAmount()).isEqualByComparingTo("185000");
        assertThat(r.getRemainingDebt()).isEqualByComparingTo("0");
        assertThat(r.isTransferred()).isTrue();
        assertThat(r.getReference()).startsWith("MOCK-TRANSFER-");
        assertThat(r.getDailyBalancesSettled()).isEqualTo(1);

        ProviderAccount account = accounts.findByProviderId(PROVIDER).orElseThrow();
        assertThat(account.getPayableBalance().getAmount()).isEqualByComparingTo("0");
        assertThat(account.getReceivableBalance().getAmount()).isEqualByComparingTo("0");
        List<ProviderLedgerEntry> added = account.getLedgerEntries().subList(4, account.getLedgerEntries().size());
        assertThat(added).extracting(ProviderLedgerEntry::getType)
                .containsExactlyInAnyOrder(LedgerEntryType.COMPENSATION_ADJUSTMENT, LedgerEntryType.PAYOUT);
        assertThat(added).filteredOn(e -> e.getType() == LedgerEntryType.COMPENSATION_ADJUSTMENT)
                .singleElement().satisfies(e -> {
                    assertThat(e.getAmount().getAmount()).isEqualByComparingTo("1300");
                    assertThat(e.getDirection()).isEqualTo(LedgerDirection.DEBIT);
                });
        assertThat(added).filteredOn(e -> e.getType() == LedgerEntryType.PAYOUT)
                .singleElement().satisfies(e -> assertThat(e.getAmount().getAmount()).isEqualByComparingTo("185000"));

        assertThat(payouts.store).hasSize(1);
        Payout payout = payouts.store.values().iterator().next();
        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
        assertThat(payout.getExternalReference()).startsWith("MOCK-TRANSFER-");
        assertThat(payout.getAmount().getAmount()).isEqualByComparingTo("185000");

        assertThat(settlements.store).hasSize(1);
        Settlement s = settlements.store.values().iterator().next();
        assertThat(s.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(s.getExecutedAt()).isNotNull();
        assertThat(s.getScheduledAt()).isEqualTo(Instant.parse("2026-11-10T05:00:00Z"));
        assertThat(s.getPeriodStart()).isEqualTo(InMemoryFinanceRepos.DAY_26_OCT_A);
        assertThat(s.getPeriodEnd()).isEqualTo(Instant.parse("2026-11-09T05:00:00Z"));
        assertThat(s.getDailyBalanceIds()).containsExactly(db.getId());

        DailyBalance settled = dailyBalances.store.get(db.getId());
        assertThat(settled.isSettled()).isTrue();
        assertThat(settled.getPendingPayout().getAmount()).isEqualByComparingTo("0");
        assertThat(settled.getSettlementId()).isEqualTo(s.getId());

        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(any()); // SettlementCreated + PayoutCompleted
    }

    @Test
    @DisplayName("Idempotencia: la segunda ejecucion no paga dos veces")
    void secondRunDoesNotPayTwice() {
        accounts.save(InMemoryFinanceRepos.exampleAccount(PROVIDER));
        service.executeForProvider(PROVIDER, CUTOFF);
        int ledgerSize = accounts.store.get(PROVIDER).getLedgerEntries().size();

        SettlementRunResult again = service.executeForProvider(PROVIDER, CUTOFF);
        assertThat(again.getOutcome()).isEqualTo(Outcome.ALREADY_SETTLED);
        assertThat(again.getNetAmount()).isEqualByComparingTo("185000");
        assertThat(again.getReference()).startsWith("MOCK-TRANSFER-");

        // Otro corte posterior sin ganancias nuevas tampoco liquida nada.
        SettlementRunResult later = service.executeForProvider(PROVIDER, LocalDate.of(2026, 11, 15));
        assertThat(later.getOutcome()).isEqualTo(Outcome.NOTHING_TO_SETTLE);
        // Un corte anterior ya cubierto tampoco.
        SettlementRunResult earlier = service.executeForProvider(PROVIDER, LocalDate.of(2026, 11, 1));
        assertThat(earlier.getOutcome()).isEqualTo(Outcome.NOTHING_TO_SETTLE);

        assertThat(payouts.store).hasSize(1);
        assertThat(settlements.store).hasSize(1);
        assertThat(accounts.store.get(PROVIDER).getLedgerEntries()).hasSize(ledgerSize);
    }

    @Test
    @DisplayName("Arrastre: lo ganado despues del corte entra en la liquidacion del martes siguiente")
    void carriesOverEarningsAfterCutoff() {
        List<ProviderLedgerEntry> entries = new ArrayList<>(InMemoryFinanceRepos.exampleAccount(PROVIDER).getLedgerEntries());
        // Lunes 9-nov 10:00 Bogota: despues del corte del domingo 8.
        entries.add(InMemoryFinanceRepos.entry(PROVIDER, LedgerEntryType.SERVICE_EARNING, 50000,
                LedgerDirection.CREDIT, "TX-5", Instant.parse("2026-11-09T15:00:00Z")));
        accounts.save(InMemoryFinanceRepos.account(PROVIDER, 236300, 1300, entries));

        SettlementRunResult first = service.executeForProvider(PROVIDER, CUTOFF);
        assertThat(first.getGrossAmount()).isEqualByComparingTo("186300");
        assertThat(first.getNetAmount()).isEqualByComparingTo("185000");
        assertThat(accounts.store.get(PROVIDER).getPayableBalance().getAmount()).isEqualByComparingTo("50000");

        SettlementRunResult second = service.executeForProvider(PROVIDER, LocalDate.of(2026, 11, 15));
        assertThat(second.getOutcome()).isEqualTo(Outcome.SETTLED);
        assertThat(second.getSettlementCode()).isEqualTo("SET-1-20261115");
        assertThat(second.getGrossAmount()).isEqualByComparingTo("50000");
        assertThat(second.getNetAmount()).isEqualByComparingTo("50000");
        assertThat(second.getPeriodStart()).isEqualTo(Instant.parse("2026-11-09T05:00:00Z"));
        assertThat(accounts.store.get(PROVIDER).getPayableBalance().getAmount()).isEqualByComparingTo("0");
        assertThat(payouts.store).hasSize(2);
    }

    @Test
    @DisplayName("Neto <= 0 (deuda >= bruto): no se transfiere y la deuda restante queda pendiente")
    void netZeroDoesNotTransfer() {
        List<ProviderLedgerEntry> entries = List.of(InMemoryFinanceRepos.entry(PROVIDER, LedgerEntryType.SERVICE_EARNING,
                1000, LedgerDirection.CREDIT, "TX-1", InMemoryFinanceRepos.DAY_26_OCT_A));
        accounts.save(InMemoryFinanceRepos.account(PROVIDER, 1000, 5000, entries));

        SettlementRunResult r = service.executeForProvider(PROVIDER, CUTOFF);

        assertThat(r.getOutcome()).isEqualTo(Outcome.NET_ZERO_NO_TRANSFER);
        assertThat(r.isTransferred()).isFalse();
        assertThat(r.getGrossAmount()).isEqualByComparingTo("1000");
        assertThat(r.getDebtCompensationAmount()).isEqualByComparingTo("1000");
        assertThat(r.getNetAmount()).isEqualByComparingTo("0");
        assertThat(r.getRemainingDebt()).isEqualByComparingTo("4000");
        verify(gateway, never()).transfer(any());
        assertThat(payouts.store).isEmpty();
        ProviderAccount account = accounts.store.get(PROVIDER);
        assertThat(account.getPayableBalance().getAmount()).isEqualByComparingTo("0");
        assertThat(account.getReceivableBalance().getAmount()).isEqualByComparingTo("4000");
        assertThat(settlements.store.values().iterator().next().getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    @DisplayName("Sin pasarela y neto > 0 => IllegalStateException sin cambios")
    void noGatewayFails() {
        accounts.save(InMemoryFinanceRepos.exampleAccount(PROVIDER));
        accounts.saves = 0;

        assertThatThrownBy(() -> serviceWithoutGateway.executeForProvider(PROVIDER, CUTOFF))
                .isInstanceOf(IllegalStateException.class);

        assertThat(accounts.saves).isZero();
        assertThat(settlements.store).isEmpty();
        assertThat(payouts.store).isEmpty();
    }

    @Test
    @DisplayName("Falla de la pasarela => excepcion y nada se guarda")
    void gatewayFailureRollsBack() {
        accounts.save(InMemoryFinanceRepos.exampleAccount(PROVIDER));
        accounts.saves = 0;
        org.mockito.Mockito.doReturn(new PayoutGatewayPort.PayoutTransferResult(false, null, "banco caido"))
                .when(gateway).transfer(any());

        assertThatThrownBy(() -> service.executeForProvider(PROVIDER, CUTOFF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("banco caido");

        assertThat(accounts.saves).isZero();
        assertThat(settlements.store).isEmpty();
        assertThat(payouts.store).isEmpty();
    }

    @Test
    @DisplayName("executeAll continua si un proveedor falla")
    void batchContinuesAfterFailure() {
        ExecuteSettlementUseCase single = mock(ExecuteSettlementUseCase.class);
        when(single.executeForProvider(1L, CUTOFF)).thenThrow(new IllegalStateException("boom"));
        when(single.executeForProvider(2L, CUTOFF)).thenReturn(SettlementRunResult.builder()
                .providerId(2L).outcome(Outcome.SETTLED).build());
        Accounts two = new Accounts();
        two.save(InMemoryFinanceRepos.exampleAccount(1L));
        two.save(InMemoryFinanceRepos.exampleAccount(2L));
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:23:00Z"), ZoneId.of("America/Bogota"));

        SettlementBatchResult batch = new SettlementBatchService(two, single, clock).executeAll(null);

        assertThat(batch.getCutoffDate()).isEqualTo(CUTOFF);
        assertThat(batch.getResults()).extracting(SettlementRunResult::getOutcome)
                .containsExactly(Outcome.FAILED, Outcome.SETTLED);
        assertThat(batch.getResults().get(0).getMessage()).isEqualTo("boom");
        assertThat(batch.count(Outcome.FAILED)).isEqualTo(1);
        assertThat(BigDecimal.ZERO).isEqualByComparingTo("0");
    }
}

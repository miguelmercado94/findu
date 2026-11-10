package com.findu.transaction.application.service.finance;

import com.findu.transaction.application.port.in.finance.dto.PendingBalanceView;
import com.findu.transaction.application.port.out.persistence.ServiceTransactionRepository;
import com.findu.transaction.application.service.InMemoryFinanceRepos;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Accounts;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Payouts;
import com.findu.transaction.application.service.InMemoryFinanceRepos.Settlements;
import com.findu.transaction.application.service.SettlementExecutionService;
import com.findu.transaction.application.service.InMemoryFinanceRepos.DailyBalances;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.gateway.PayoutGatewayPort;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.enums.TransactionStatus;
import com.findu.transaction.infrastructure.adapter.out.gateway.MockPayoutGatewayAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingBalanceServiceTest {

    private static final Long PROVIDER = 1L;
    private Accounts accounts;
    private Settlements settlements;
    private Payouts payouts;
    private ServiceTransactionRepository txRepo;
    private PendingBalanceService service;

    @BeforeEach
    void setUp() {
        accounts = new Accounts();
        settlements = new Settlements();
        payouts = new Payouts();
        txRepo = mock(ServiceTransactionRepository.class);
        service = new PendingBalanceService(accounts, settlements, payouts, txRepo);
        accounts.save(InMemoryFinanceRepos.exampleAccount(PROVIDER));
    }

    private ServiceTransaction tx(Instant createdAt, TransactionStatus status) {
        ServiceTransaction tx = mock(ServiceTransaction.class);
        when(tx.getCreatedAt()).thenReturn(createdAt);
        when(tx.getTransactionStatus()).thenReturn(status);
        return tx;
    }

    @Test
    @DisplayName("Antes de liquidar: neto unico 185000, arrastrado, 4 pendientes, proximo martes 17-nov")
    void beforeSettlement() {
        List<ServiceTransaction> txs = List.of(
                tx(InMemoryFinanceRepos.DAY_26_OCT_A, TransactionStatus.REGISTERED),
                tx(InMemoryFinanceRepos.DAY_26_OCT_A, TransactionStatus.REGISTERED),
                tx(InMemoryFinanceRepos.DAY_26_OCT_B, TransactionStatus.REGISTERED),
                tx(InMemoryFinanceRepos.DAY_26_OCT_B, TransactionStatus.REGISTERED),
                tx(InMemoryFinanceRepos.DAY_26_OCT_B, TransactionStatus.CANCELLED));
        when(txRepo.findByProviderId(PROVIDER)).thenReturn(txs);

        PendingBalanceView v = service.getPendingBalance(PROVIDER, LocalDate.of(2026, 11, 10));

        assertThat(v.getGrossAmount().getAmount()).isEqualByComparingTo("186300");
        assertThat(v.getDebtCompensationAmount().getAmount()).isEqualByComparingTo("1300");
        assertThat(v.getNetAmount().getAmount()).isEqualByComparingTo("185000");
        assertThat(v.getRemainingDebt().getAmount()).isEqualByComparingTo("0");
        assertThat(v.isCarriedOver()).isTrue();
        assertThat(v.getCarriedOverGrossAmount().getAmount()).isEqualByComparingTo("186300");
        assertThat(v.getThisWeekGrossAmount().getAmount()).isEqualByComparingTo("0");
        assertThat(v.getPendingServicesCount()).isEqualTo(4);
        assertThat(v.getNextSettlementDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(v.getNextSettlementCutoff()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(v.getNextSettlementNet().getAmount()).isEqualByComparingTo("185000");
        assertThat(v.getLastSettlement()).isNull();
    }

    @Test
    @DisplayName("Despues de liquidar: todo en 0, lastSettlement con referencia, proximo martes igual")
    void afterSettlement() {
        List<ServiceTransaction> txs = List.of(tx(InMemoryFinanceRepos.DAY_26_OCT_A, TransactionStatus.REGISTERED));
        when(txRepo.findByProviderId(PROVIDER)).thenReturn(txs);
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:23:00Z"), ZoneId.of("America/Bogota"));
        @SuppressWarnings("unchecked")
        ObjectProvider<PayoutGatewayPort> gw = mock(ObjectProvider.class);
        when(gw.getIfAvailable()).thenReturn(new MockPayoutGatewayAdapter());
        new SettlementExecutionService(accounts, settlements, new DailyBalances(), payouts, service, gw,
                mock(EventPublisherPort.class), mock(TransactionEventPublisher.class), clock)
                .executeForProvider(PROVIDER, null);

        PendingBalanceView v = service.getPendingBalance(PROVIDER, LocalDate.of(2026, 11, 10));

        assertThat(v.getGrossAmount().getAmount()).isEqualByComparingTo("0");
        assertThat(v.getDebtCompensationAmount().getAmount()).isEqualByComparingTo("0");
        assertThat(v.getNetAmount().getAmount()).isEqualByComparingTo("0");
        assertThat(v.isCarriedOver()).isFalse();
        assertThat(v.getPendingServicesCount()).isZero();
        assertThat(v.getNextSettlementDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(v.getNextSettlementNet().getAmount()).isEqualByComparingTo("0");
        assertThat(v.getLastSettlement()).isNotNull();
        assertThat(v.getLastSettlement().getStatus()).isEqualTo("COMPLETED");
        assertThat(v.getLastSettlement().getNetAmount().getAmount()).isEqualByComparingTo("185000");
        assertThat(v.getLastSettlement().getReference()).startsWith("MOCK-TRANSFER-");
    }

    @Test
    @DisplayName("Lunes 26-oct con ganancias del 26: la proxima liquidacion salta al martes 3-nov")
    void mondayEarningsSkipToFollowingTuesday() {
        when(txRepo.findByProviderId(PROVIDER)).thenReturn(List.of());

        PendingBalanceView v = service.getPendingBalance(PROVIDER, LocalDate.of(2026, 10, 26));

        assertThat(v.getNextSettlementDate()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(v.getNextSettlementCutoff()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(v.getNextSettlementNet().getAmount()).isEqualByComparingTo("185000");
        // Es de esta semana (lunes 26 en adelante): no es arrastre.
        assertThat(v.isCarriedOver()).isFalse();
        assertThat(v.getThisWeekGrossAmount().getAmount()).isEqualByComparingTo("186300");
    }

    @Test
    @DisplayName("Proveedor sin cuenta: todo en cero y proximo martes")
    void noAccount() {
        when(txRepo.findByProviderId(99L)).thenReturn(List.of());
        PendingBalanceView v = service.getPendingBalance(99L, LocalDate.of(2026, 11, 10));
        assertThat(v.getNetAmount().getAmount()).isEqualByComparingTo("0");
        assertThat(v.getNextSettlementDate()).isEqualTo(LocalDate.of(2026, 11, 17));
    }
}

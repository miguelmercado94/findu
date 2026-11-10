package com.findu.transaction.application.service.finance;

import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.application.port.in.finance.dto.DebtStatusResult;
import com.findu.transaction.application.port.in.finance.dto.FinancialMovementsResult;
import com.findu.transaction.application.port.in.finance.dto.PendingBalanceView;
import com.findu.transaction.application.port.in.finance.dto.WeeklyBalanceResult;
import com.findu.transaction.application.service.InMemoryFinanceRepos;
import com.findu.transaction.domain.model.payout.Payout;
import com.findu.transaction.application.port.out.gateway.BreBPaymentGatewayPort;
import com.findu.transaction.application.port.out.persistence.*;
import com.findu.transaction.domain.enums.*;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.debt.ProviderDebtPayment;
import com.findu.transaction.domain.model.finance.BankConsignmentReport;
import com.findu.transaction.domain.model.finance.BreBPaymentOrder;
import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;
import com.findu.transaction.domain.model.transaction.ServiceTransaction;
import com.findu.transaction.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderFinanceServiceTest {

    @Mock
    private ProviderAccountRepository providerAccountRepository;

    @Mock
    private ProviderSettlementProfileRepository profileRepository;

    @Mock
    private DailyBalanceRepository dailyBalanceRepository;

    @Mock
    private ServiceTransactionRepository serviceTransactionRepository;

    @Mock
    private ProviderDebtPaymentRepository providerDebtPaymentRepository;

    @Mock
    private BreBPaymentOrderRepository breBOrderRepository;

    @Mock
    private BankConsignmentReportRepository bankConsignmentRepository;

    @Mock
    private BreBPaymentGatewayPort breBGatewayPort;

    @Mock
    private com.findu.transaction.infrastructure.adapter.out.client.ValidationBrebClient validationBrebClient;

    @Mock
    private com.findu.transaction.infrastructure.adapter.out.client.CoreServiceClient coreServiceClient;

    @Mock
    private PendingBalanceService pendingBalanceService;
    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private ProviderFinanceService financeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(financeService, "companyBankName", "Bancolombia");
        ReflectionTestUtils.setField(financeService, "companyAccountType", "Ahorros");
        ReflectionTestUtils.setField(financeService, "companyAccountNumber", "031-987654-32");
        ReflectionTestUtils.setField(financeService, "companyAccountHolder", "FindU S.A.S.");
        ReflectionTestUtils.setField(financeService, "companyNit", "901.765.432-1");
        ReflectionTestUtils.setField(financeService, "defaultDebtLimit", new BigDecimal("125000.00"));
    }

    @Test
    @DisplayName("Debe calcular el balance semanal con corte domingo y próxima liquidación martes")
    void shouldCalculateWeeklyBalanceCorrectly() {
        Long providerId = 100L;
        LocalDate testDate = LocalDate.of(2026, 10, 20); // Martes

        ProviderAccount account = ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL);
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(serviceTransactionRepository.findByProviderId(providerId)).thenReturn(Collections.emptyList());
        when(pendingBalanceService.getPendingBalance(eq(providerId), any(LocalDate.class)))
                .thenReturn(pending(0, 0, LocalDate.of(2026, 10, 27)));

        WeeklyBalanceResult result = financeService.getWeeklyBalance(providerId, testDate, "miguelm");

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 10, 19), result.getStartDate()); // Lunes
        assertEquals(LocalDate.of(2026, 10, 25), result.getEndDate());   // Domingo
        assertEquals(LocalDate.of(2026, 10, 27), result.getNextSettlementDate()); // Martes siguiente
        assertEquals("PROGRAMADO", result.getNextSettlementStatus());
        assertEquals("El corte se realiza el domingo", result.getCutoffNote());
    }

    @Test
    @DisplayName("Debe detectar estado de advertencia cuando la deuda supera el 80% del límite")
    void shouldDetectWarningRestrictionStatus() {
        Long providerId = 100L;
        // Deuda de 105.000 COP sobre límite de 125.000 COP = 84%
        ProviderAccount account = ProviderAccount.reconstitute(
                "acc-1", providerId, ProviderType.PERSONA_NATURAL,
                Money.ZERO, Money.of(105000), Money.of(125000),
                ProviderAccountStatus.NORMAL, Collections.emptyList(),
                java.time.Instant.now(), java.time.Instant.now()
        );

        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(profileRepository.findByProviderId(providerId)).thenReturn(Optional.empty());

        DebtStatusResult debt = financeService.getDebtStatus(providerId);

        assertNotNull(debt);
        assertEquals(FinanceRestrictionStatus.WARNING, debt.getRestrictionStatus());
        assertFalse(debt.isBlocked());
        assertTrue(debt.getUsedPercentage() >= 80.0);
        assertEquals(new BigDecimal("20000.00"), debt.getRemainingBeforeBlock().getAmount());
    }

    @Test
    @DisplayName("Debe detectar estado bloqueado cuando la deuda supera el 100% del límite")
    void shouldDetectRestrictedStatusWhenOverLimit() {
        Long providerId = 100L;
        // Deuda de 130.000 COP sobre límite de 125.000 COP
        ProviderAccount account = ProviderAccount.reconstitute(
                "acc-2", providerId, ProviderType.PERSONA_NATURAL,
                Money.ZERO, Money.of(130000), Money.of(125000),
                ProviderAccountStatus.RESTRICTED, Collections.emptyList(),
                java.time.Instant.now(), java.time.Instant.now()
        );

        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(profileRepository.findByProviderId(providerId)).thenReturn(Optional.empty());

        DebtStatusResult debt = financeService.getDebtStatus(providerId);

        assertNotNull(debt);
        assertEquals(FinanceRestrictionStatus.RESTRICTED, debt.getRestrictionStatus());
        assertTrue(debt.isBlocked());
        assertEquals(new BigDecimal("0.00"), debt.getRemainingBeforeBlock().getAmount());
    }

    @Test
    @DisplayName("Debe crear una orden Bre-B con llave personalizada y QR dinámico")
    void shouldCreateBreBOrderSuccessfully() {
        Long providerId = 100L;
        Money amount = Money.of(85000);

        ProviderAccount account = ProviderAccount.reconstitute(
                "acc-3", providerId, ProviderType.PERSONA_NATURAL,
                Money.ZERO, Money.of(85000), Money.of(125000),
                ProviderAccountStatus.NORMAL, Collections.emptyList(),
                java.time.Instant.now(), java.time.Instant.now()
        );
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));

        when(breBGatewayPort.generatePersonalizedKey(eq(providerId), eq("miguelm"))).thenReturn("miguelm-784529");
        when(breBGatewayPort.generateDynamicQrPayload(anyString(), any(), anyString(), anyString())).thenReturn("data:bre-b/qr;base64,XYZ123");
        when(breBOrderRepository.save(any(BreBPaymentOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBreBOrderCommand cmd = CreateBreBOrderCommand.builder()
                .providerId(providerId)
                .amount(amount)
                .username("miguelm")
                .build();

        BreBPaymentOrder order = financeService.createOrder(cmd);

        assertNotNull(order);
        assertEquals(providerId, order.getProviderId());
        assertEquals(amount.getAmount(), order.getAmount().getAmount());
        assertEquals("miguelm-784529", order.getBreBKey());
        assertEquals(BreBOrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getExpiresAt());
    }

    @Test
    @DisplayName("Debe procesar confirmación por webhook Bre-B de forma idempotente y reducir deuda")
    void shouldProcessBreBWebhookAndReduceDebt() {
        Long providerId = 100L;
        Money amount = Money.of(85000);

        BreBPaymentOrder order = BreBPaymentOrder.create(
                providerId, amount, "miguelm-784529", "qr", Duration.ofMinutes(10));

        ProviderAccount account = ProviderAccount.reconstitute(
                "acc-4", providerId, ProviderType.PERSONA_NATURAL,
                Money.ZERO, amount, Money.of(125000),
                ProviderAccountStatus.NORMAL, Collections.emptyList(),
                java.time.Instant.now(), java.time.Instant.now()
        );
        assertEquals(amount.getAmount(), account.getReceivableBalance().getAmount());

        when(breBGatewayPort.validateWebhookSignature(any(), any())).thenReturn(true);
        when(breBOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(breBOrderRepository.save(any(BreBPaymentOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(providerAccountRepository.save(any(ProviderAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessBreBWebhookCommand cmd = ProcessBreBWebhookCommand.builder()
                .orderId(order.getId())
                .transactionId("TX-BREB-SUCCESS-123")
                .amount(amount)
                .status("CONFIRMED")
                .signature("valid-sig")
                .rawPayload("{}")
                .build();

        BreBPaymentOrder confirmed = financeService.processWebhook(cmd);

        assertNotNull(confirmed);
        assertEquals(BreBOrderStatus.CONFIRMED, confirmed.getStatus());
        assertEquals("TX-BREB-SUCCESS-123", confirmed.getExternalTransactionId());

        // Verificar que la deuda se redujo a cero en la cuenta
        assertEquals(new BigDecimal("0.00"), account.getReceivableBalance().getAmount());
        verify(providerDebtPaymentRepository, times(1)).save(any(ProviderDebtPayment.class));
    }

    @Test
    @DisplayName("Debe confirmar consignación bancaria y aplicar pago a la deuda")
    void shouldConfirmBankConsignmentSuccessfully() {
        Long providerId = 100L;
        Money amount = Money.of(50000);
        String ref = "miguelm-BANCOLOMBIA-1234";

        ProviderAccount account = ProviderAccount.reconstitute(
                "acc-5", providerId, ProviderType.PERSONA_NATURAL,
                Money.ZERO, Money.of(85000), Money.of(125000),
                ProviderAccountStatus.NORMAL, Collections.emptyList(),
                java.time.Instant.now(), java.time.Instant.now()
        );

        when(bankConsignmentRepository.findByReference(ref)).thenReturn(Optional.empty());
        when(bankConsignmentRepository.save(any(BankConsignmentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(providerAccountRepository.save(any(ProviderAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmBankConsignmentCommand cmd = ConfirmBankConsignmentCommand.builder()
                .providerId(providerId)
                .amount(amount)
                .reference(ref)
                .bankName("Bancolombia")
                .confirmedBy("ADMIN_OPERATIONS")
                .notes("Conciliación extracto bancario")
                .build();

        BankConsignmentReport report = financeService.confirmConsignment(cmd);

        assertNotNull(report);
        assertEquals(PaymentStatus.COMPLETED, report.getStatus());
        assertEquals("ADMIN_OPERATIONS", report.getConfirmedBy());
        assertEquals(new BigDecimal("35000.00"), account.getReceivableBalance().getAmount());
    }

    @Test
    @DisplayName("Debe consultar el detalle financiero de un servicio con medio de cobro correctamente")
    void testGetServiceFinancialDetail_Success() {
        Long providerId = 100L;
        Long serviceRequestId = 10482L;

        ServiceTransaction tx = ServiceTransaction.create(
                serviceRequestId,
                providerId,
                50L,
                Money.of(new BigDecimal("80000.00")),
                new BigDecimal("0.10"),
                Money.ZERO,
                Money.ZERO,
                PaymentMethod.TRANSFER
        );

        when(serviceTransactionRepository.findByServiceRequestId(serviceRequestId))
                .thenReturn(Optional.of(tx));
        when(coreServiceClient.getSolicitud(serviceRequestId))
                .thenReturn(Optional.of(com.findu.transaction.infrastructure.adapter.out.client.CoreServiceClient.SolicitudCoreDto.builder()
                        .id(serviceRequestId)
                        .servicioNombre("Enfermería a domicilio")
                        .categoriaNombre("Cuidado de adulto mayor")
                        .build()));

        var response = financeService.getServiceFinancialDetail(providerId, serviceRequestId);

        assertNotNull(response);
        assertEquals(serviceRequestId, response.getIdentificacion().getServiceRequestId());
        assertEquals("Enfermería a domicilio", response.getIdentificacion().getServicioNombre());
        assertEquals("Cuidado de adulto mayor", response.getIdentificacion().getEspecialidadNombre());
        assertEquals(new BigDecimal("80000.00"), response.getResumenEconomico().getTotalCobradoCliente());
        assertEquals(new BigDecimal("72000.00"), response.getLiquidacion().getGananciaNetaProveedor());
        assertEquals(new BigDecimal("-8000.00"), response.getLiquidacion().getComisionFinduMonto());
        assertNotNull(response.getMedioCobro());
        assertEquals("Transferencia bancaria", response.getMedioCobro().getMetodo());
        assertEquals("Confirmado", response.getMedioCobro().getEstado());
    }

    // ---------- Saldo neto unico (FEAT-2) ----------

    private static PendingBalanceView pending(long gross, long compensation, LocalDate nextDate) {
        long net = gross - compensation;
        return PendingBalanceView.builder()
                .grossAmount(Money.of(gross)).debtCompensationAmount(Money.of(compensation)).netAmount(Money.of(net))
                .remainingDebt(Money.ZERO).thisWeekGrossAmount(Money.ZERO).carriedOverGrossAmount(Money.of(gross))
                .carriedOver(gross > 0).pendingServicesCount(gross > 0 ? 4 : 0)
                .nextSettlementDate(nextDate).nextSettlementCutoff(nextDate.minusDays(2)).nextSettlementNet(Money.of(net))
                .build();
    }

    @Test
    @DisplayName("Weekly con el ejemplo: provisionalBalance == proxima liquidacion == 185000 y desglose 186300/1300/185000")
    void weeklyUsesSingleNetBalance() {
        Long providerId = 1L;
        ProviderAccount account = InMemoryFinanceRepos.exampleAccount(providerId);
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(profileRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(serviceTransactionRepository.findByProviderId(providerId)).thenReturn(Collections.emptyList());
        when(pendingBalanceService.getPendingBalance(eq(providerId), any(LocalDate.class)))
                .thenReturn(pending(186300, 1300, LocalDate.of(2026, 11, 17)));

        WeeklyBalanceResult r = financeService.getWeeklyBalance(providerId, LocalDate.of(2026, 11, 10), "x");

        assertEquals(0, new BigDecimal("185000").compareTo(r.getProvisionalBalance().getAmount()));
        assertEquals(0, new BigDecimal("185000").compareTo(r.getNextSettlementEstimatedAmount().getAmount()));
        assertEquals(LocalDate.of(2026, 11, 17), r.getNextSettlementDate());
        assertEquals(4, r.getPendingSettlementCount());
        assertEquals(0, new BigDecimal("186300").compareTo(r.getPendingBalance().getGrossAmount().getAmount()));
        assertEquals(0, new BigDecimal("1300").compareTo(r.getPendingBalance().getDebtCompensationAmount().getAmount()));
        assertTrue(r.getPendingBalance().isCarriedOver());
    }

    @Test
    @DisplayName("Movements con el ejemplo: Saldo del periodo == saldo pendiente neto 185000; comision en efectivo clara y negativa")
    void movementsMatchPendingBalanceAndCashCommissionIsClear() {
        Long providerId = 1L;
        ProviderAccount account = InMemoryFinanceRepos.exampleAccount(providerId);
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        PendingBalanceView pending = pending(186300, 1300, LocalDate.of(2026, 11, 17));
        when(pendingBalanceService.getPendingBalance(eq(providerId), any(LocalDate.class))).thenReturn(pending);

        FinancialMovementsResult r = financeService.getMovements(providerId, "ALL");

        assertEquals(0, r.getNetBalance().getAmount().compareTo(r.getPendingBalance().getNetAmount().getAmount()));
        assertEquals(0, new BigDecimal("185000").compareTo(r.getNetBalance().getAmount()));
        var cash = r.getMovements().stream().filter(m -> "CASH_COMMISSION_DEBT".equals(m.getMovementType())).toList();
        assertEquals(2, cash.size());
        cash.forEach(m -> {
            assertEquals("Comisión por servicio en efectivo", m.getTitle());
            assertEquals("CASH_PAYMENT_DEBT", m.getType());
            assertTrue(m.getAmount().getAmount().signum() < 0);
            assertFalse(m.isIncome());
        });
        assertTrue(r.getMovements().stream().anyMatch(m -> "SERVICE_EARNING".equals(m.getMovementType())));
    }

    @Test
    @DisplayName("Tras liquidar: pendingBalance 0, item SETTLEMENT_PAYOUT con referencia MOCK-TRANSFER y netBalance sigue en 185000")
    void movementsAfterSettlement() {
        Long providerId = 1L;
        ProviderAccount base = InMemoryFinanceRepos.exampleAccount(providerId);
        java.util.List<com.findu.transaction.domain.model.account.ProviderLedgerEntry> entries = new java.util.ArrayList<>(base.getLedgerEntries());
        java.time.Instant at = java.time.Instant.parse("2026-11-10T15:23:00Z");
        entries.add(InMemoryFinanceRepos.entry(providerId, LedgerEntryType.COMPENSATION_ADJUSTMENT, 1300, LedgerDirection.DEBIT, "COMP-1", at));
        entries.add(InMemoryFinanceRepos.entry(providerId, LedgerEntryType.PAYOUT, 185000, LedgerDirection.DEBIT, "PO-1-1", at));
        ProviderAccount account = InMemoryFinanceRepos.account(providerId, 0, 0, entries);
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(pendingBalanceService.getPendingBalance(eq(providerId), any(LocalDate.class)))
                .thenReturn(pending(0, 0, LocalDate.of(2026, 11, 17)));
        Payout payout = Payout.reconstitute("p1", "PO-1-1", "S-1", providerId, Money.of(185000), PayoutStatus.COMPLETED,
                "MOCK-TRANSFER-abc", null, at, at, at);
        when(payoutRepository.findByPayoutCode("PO-1-1")).thenReturn(Optional.of(payout));
        when(settlementRepository.findById("S-1")).thenReturn(Optional.of(
                com.findu.transaction.domain.model.settlement.Settlement.createExecuted("SET-1-20261108", providerId,
                        ProviderType.PERSONA_NATURAL, SettlementType.WEEKLY, Money.of(186300), Money.of(1300), at, at, at, null)));

        FinancialMovementsResult r = financeService.getMovements(providerId, "ALL");

        assertEquals(0, r.getPendingBalance().getNetAmount().getAmount().signum());
        assertEquals(0, new BigDecimal("185000").compareTo(r.getNetBalance().getAmount()));
        var payoutItem = r.getMovements().stream().filter(m -> "SETTLEMENT_PAYOUT".equals(m.getMovementType())).findFirst().orElseThrow();
        assertEquals("Liquidación transferida", payoutItem.getTitle());
        assertEquals("MOCK-TRANSFER-abc", payoutItem.getReference());
        assertEquals("SET-1-20261108", payoutItem.getSettlementCode());
        assertEquals("COMPLETADO", payoutItem.getStatus());
        assertTrue(payoutItem.getAmount().getAmount().signum() > 0);
        var comp = r.getMovements().stream().filter(m -> "DEBT_COMPENSATION".equals(m.getMovementType())).findFirst().orElseThrow();
        assertTrue(comp.getAmount().getAmount().signum() < 0);
    }

    @Test
    @DisplayName("Deuda mayor al saldo mantiene provisionalBalance negativo y la proxima liquidacion en 0")
    void debtGreaterThanBalanceKeepsNegativeProvisional() {
        Long providerId = 1L;
        ProviderAccount account = InMemoryFinanceRepos.account(providerId, 0, 105000, Collections.emptyList());
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));
        when(profileRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(serviceTransactionRepository.findByProviderId(providerId)).thenReturn(Collections.emptyList());
        when(pendingBalanceService.getPendingBalance(eq(providerId), any(LocalDate.class)))
                .thenReturn(pending(0, 0, LocalDate.of(2026, 11, 17)));

        WeeklyBalanceResult r = financeService.getWeeklyBalance(providerId, LocalDate.of(2026, 11, 10), "x");

        assertEquals(0, new BigDecimal("-105000").compareTo(r.getProvisionalBalance().getAmount()));
        assertEquals("NEGATIVE_WARNING", r.getBalanceType());
        assertEquals(0, r.getNextSettlementEstimatedAmount().getAmount().signum());
    }
}
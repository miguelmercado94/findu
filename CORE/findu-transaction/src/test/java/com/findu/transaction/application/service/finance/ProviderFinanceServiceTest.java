package com.findu.transaction.application.service.finance;

import com.findu.transaction.application.port.in.finance.command.ConfirmBankConsignmentCommand;
import com.findu.transaction.application.port.in.finance.command.CreateBreBOrderCommand;
import com.findu.transaction.application.port.in.finance.command.ProcessBreBWebhookCommand;
import com.findu.transaction.application.port.in.finance.dto.DebtStatusResult;
import com.findu.transaction.application.port.in.finance.dto.WeeklyBalanceResult;
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
}

package com.findu.transaction.dailybalance;

import com.findu.transaction.dailybalance.entity.DailyBalanceEntity;
import com.findu.transaction.dailybalance.entity.ExtraExpenseRequestEntity;
import com.findu.transaction.dailybalance.entity.ProviderAccountEntity;
import com.findu.transaction.dailybalance.entity.ServiceTransactionEntity;
import com.findu.transaction.dailybalance.model.DailyBalanceExecutionResult;
import com.findu.transaction.dailybalance.repository.DailyBalanceJpaRepository;
import com.findu.transaction.dailybalance.repository.ExtraExpenseQueryRepository;
import com.findu.transaction.dailybalance.repository.ProviderAccountQueryRepository;
import com.findu.transaction.dailybalance.repository.ServiceTransactionQueryRepository;
import com.findu.transaction.dailybalance.service.DailyBalanceProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyBalanceProcessorTest {

    @Mock private DailyBalanceJpaRepository dailyBalanceJpaRepository;
    @Mock private ServiceTransactionQueryRepository transactionQueryRepository;
    @Mock private ExtraExpenseQueryRepository extraExpenseQueryRepository;
    @Mock private ProviderAccountQueryRepository providerAccountQueryRepository;

    @InjectMocks
    private DailyBalanceProcessor processor;

    private final LocalDate targetDate = LocalDate.of(2026, 9, 30);
    private final Long providerId = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "timezoneStr", "America/Bogota");
    }

    @Test
    @DisplayName("Corte diario para proveedor sin actividad debe producir balance en cero")
    void executeCut_NoActivity_ShouldProduceZeroBalance() {
        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate.minusDays(1)))
                .thenReturn(Optional.empty());
        when(transactionQueryRepository.findByProviderIdAndCreatedAtBetween(eq(providerId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(extraExpenseQueryRepository.findApprovedByProviderIdAndApprovedAtBetween(eq(providerId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate))
                .thenReturn(Optional.empty());
        when(dailyBalanceJpaRepository.save(any(DailyBalanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyBalanceEntity entity = processor.processSingleProviderCut(providerId, targetDate);

        assertThat(entity).isNotNull();
        assertThat(entity.getOpeningBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(entity.getTotalEarnings()).isEqualTo(BigDecimal.ZERO);
        assertThat(entity.getTotalCommissions()).isEqualTo(BigDecimal.ZERO);
        assertThat(entity.getClosingBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(entity.getStatus()).isEqualTo("CALCULATED");
    }

    @Test
    @DisplayName("Corte diario para proveedor con transacciones CASH y TRANSFER con comisiones y tips")
    void executeCut_WithTransactions_ShouldCalculateCorrectTotals() {
        ZoneId zoneId = ZoneId.of("America/Bogota");
        Instant txTime = targetDate.atStartOfDay(zoneId).plusHours(10).toInstant();

        // Servicio 1: CASH (50,000 COP, comisión 5,000 COP)
        ServiceTransactionEntity tx1 = new ServiceTransactionEntity();
        tx1.setId("tx-1");
        tx1.setProviderId(providerId);
        tx1.setServiceAmount(new BigDecimal("50000.00"));
        tx1.setCommissionAmount(new BigDecimal("5000.00"));
        tx1.setTipAmount(BigDecimal.ZERO);
        tx1.setProviderAmount(new BigDecimal("45000.00"));
        tx1.setTotalCustomerAmount(new BigDecimal("50000.00"));
        tx1.setPaymentMethod("CASH");
        tx1.setCreatedAt(txTime);

        // Servicio 2: TRANSFER (80,000 COP, comisión 8,000 COP, tip 10,000 COP)
        ServiceTransactionEntity tx2 = new ServiceTransactionEntity();
        tx2.setId("tx-2");
        tx2.setProviderId(providerId);
        tx2.setServiceAmount(new BigDecimal("80000.00"));
        tx2.setCommissionAmount(new BigDecimal("8000.00"));
        tx2.setTipAmount(new BigDecimal("10000.00"));
        tx2.setProviderAmount(new BigDecimal("72000.00"));
        tx2.setTotalCustomerAmount(new BigDecimal("90000.00"));
        tx2.setPaymentMethod("TRANSFER");
        tx2.setCreatedAt(txTime);

        // Extra expense aprobado: 15,000 COP
        ExtraExpenseRequestEntity extra = new ExtraExpenseRequestEntity();
        extra.setId("ext-1");
        extra.setProviderId(providerId);
        extra.setAmount(new BigDecimal("15000.00"));
        extra.setStatus("APPROVED");
        extra.setApprovedAt(txTime);

        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate.minusDays(1)))
                .thenReturn(Optional.empty());
        when(transactionQueryRepository.findByProviderIdAndCreatedAtBetween(eq(providerId), any(), any()))
                .thenReturn(List.of(tx1, tx2));
        when(extraExpenseQueryRepository.findApprovedByProviderIdAndApprovedAtBetween(eq(providerId), any(), any()))
                .thenReturn(List.of(extra));
        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate))
                .thenReturn(Optional.empty());
        when(dailyBalanceJpaRepository.save(any(DailyBalanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyBalanceEntity entity = processor.processSingleProviderCut(providerId, targetDate);

        assertThat(entity).isNotNull();
        assertThat(entity.getTotalEarnings()).isEqualByComparingTo(new BigDecimal("117000.00")); // 45k + 72k
        assertThat(entity.getTotalCommissions()).isEqualByComparingTo(new BigDecimal("13000.00")); // 5k + 8k
        assertThat(entity.getTotalTips()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(entity.getTotalExtras()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(entity.getCashPayments()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(entity.getDigitalPayments()).isEqualByComparingTo(new BigDecimal("90000.00"));
        assertThat(entity.getProviderDebt()).isEqualByComparingTo(new BigDecimal("5000.00")); // comisión efectivo
        assertThat(entity.getPendingPayout()).isEqualByComparingTo(new BigDecimal("97000.00")); // 72k digital + 10k tip + 15k extra
    }

    @Test
    @DisplayName("Idempotencia: Re-ejecutar el corte para el mismo providerId y fecha actualiza en lugar de duplicar")
    void executeCut_DuplicateRun_ShouldUpdateExistingRecord() {
        DailyBalanceEntity existing = new DailyBalanceEntity();
        existing.setId("BAL-100-2026-09-30");
        existing.setProviderId(providerId);
        existing.setBalanceDate(targetDate);
        existing.setOpeningBalance(BigDecimal.ZERO);
        existing.setStatus("CALCULATED");

        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate.minusDays(1)))
                .thenReturn(Optional.empty());
        when(dailyBalanceJpaRepository.findByProviderIdAndBalanceDate(providerId, targetDate))
                .thenReturn(Optional.of(existing));
        when(dailyBalanceJpaRepository.save(any(DailyBalanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyBalanceEntity updated = processor.processSingleProviderCut(providerId, targetDate);

        assertThat(updated.getId()).isEqualTo(existing.getId());
        verify(dailyBalanceJpaRepository, times(1)).save(existing);
    }
}

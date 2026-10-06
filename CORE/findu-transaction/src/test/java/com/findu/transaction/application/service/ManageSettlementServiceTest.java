package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.application.port.out.persistence.DailyBalanceRepository;
import com.findu.transaction.application.port.out.persistence.ProviderAccountRepository;
import com.findu.transaction.application.port.out.persistence.ProviderSettlementProfileRepository;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.enums.SettlementType;
import com.findu.transaction.domain.model.account.ProviderAccount;
import com.findu.transaction.domain.model.balance.DailyBalance;
import com.findu.transaction.domain.model.settlement.ProviderSettlementProfile;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManageSettlementServiceTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private ProviderSettlementProfileRepository profileRepository;
    @Mock private DailyBalanceRepository dailyBalanceRepository;
    @Mock private ProviderAccountRepository providerAccountRepository;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private ManageSettlementService service;

    private final Long providerId = 100L;
    private final LocalDate targetDate = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setUp() {
        ProviderAccount account = ProviderAccount.create(providerId, ProviderType.PERSONA_NATURAL);
        when(providerAccountRepository.findByProviderId(providerId)).thenReturn(Optional.of(account));

        ProviderSettlementProfile profile = ProviderSettlementProfile.createDefault(providerId, ProviderType.PERSONA_NATURAL);
        when(profileRepository.findByProviderId(providerId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(ProviderSettlementProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(settlementRepository.findSettledDailyBalanceIdsByProviderId(providerId)).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Crear settlement diario para Persona Natural con saldo positivo a favor")
    void execute_DailySettlement_PersonaNatural_PositiveBalance() {
        DailyBalance balance = DailyBalance.create(
                providerId,
                targetDate,
                Money.ZERO,
                Money.of(new BigDecimal("180000.00")),
                Money.of(new BigDecimal("20000.00")),
                Money.of(new BigDecimal("10000.00")),
                Money.of(new BigDecimal("15000.00")),
                Money.of(new BigDecimal("120000.00")),
                Money.of(new BigDecimal("80000.00")),
                Money.of(new BigDecimal("12000.00")),
                Money.of(new BigDecimal("97000.00")),
                Money.of(new BigDecimal("85000.00"))
        );

        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(eq(providerId), any(), any()))
                .thenReturn(List.of(balance));

        CreateSettlementCommand cmd = CreateSettlementCommand.builder()
                .providerId(providerId)
                .targetDate(targetDate)
                .build();

        Settlement settlement = service.execute(cmd);

        assertThat(settlement).isNotNull();
        assertThat(settlement.getProviderId()).isEqualTo(providerId);
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.DAILY);
        assertThat(settlement.getGrossAmount()).isEqualTo(Money.of(new BigDecimal("97000.00")));
        assertThat(settlement.getDebtCompensationAmount()).isEqualTo(Money.of(new BigDecimal("12000.00")));
        assertThat(settlement.getNetAmount()).isEqualTo(Money.of(new BigDecimal("85000.00")));
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CREATED);

        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Crear settlement semanal para Corporativo consolidando múltiples DailyBalances")
    void execute_WeeklySettlement_Corporativo() {
        Long corpProviderId = 200L;
        ProviderAccount corpAccount = ProviderAccount.create(corpProviderId, ProviderType.CORPORATIVO);
        when(providerAccountRepository.findByProviderId(corpProviderId)).thenReturn(Optional.of(corpAccount));

        ProviderSettlementProfile corpProfile = ProviderSettlementProfile.createDefault(corpProviderId, ProviderType.CORPORATIVO);
        when(profileRepository.findByProviderId(corpProviderId)).thenReturn(Optional.of(corpProfile));
        when(settlementRepository.findSettledDailyBalanceIdsByProviderId(corpProviderId)).thenReturn(Collections.emptyList());

        DailyBalance day1 = DailyBalance.create(corpProviderId, LocalDate.of(2026, 9, 28), Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.of(new BigDecimal("100000.00")), Money.of(new BigDecimal("100000.00")));
        DailyBalance day2 = DailyBalance.create(corpProviderId, LocalDate.of(2026, 9, 29), Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.of(new BigDecimal("150000.00")), Money.of(new BigDecimal("150000.00")));

        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(eq(corpProviderId), any(), any()))
                .thenReturn(List.of(day1, day2));

        CreateSettlementCommand cmd = CreateSettlementCommand.builder()
                .providerId(corpProviderId)
                .targetDate(targetDate)
                .build();

        Settlement settlement = service.execute(cmd);

        assertThat(settlement).isNotNull();
        assertThat(settlement.getSettlementType()).isEqualTo(SettlementType.WEEKLY);
        assertThat(settlement.getGrossAmount()).isEqualTo(Money.of(new BigDecimal("250000.00")));
        assertThat(settlement.getNetAmount()).isEqualTo(Money.of(new BigDecimal("250000.00")));
        assertThat(settlement.getDailyBalanceIds()).hasSize(2);
    }

    @Test
    @DisplayName("Idempotencia: Re-ejecutar settlement para el mismo período retorna la liquidación existente")
    void execute_Idempotency_ShouldReturnExistingSettlement() {
        Settlement existing = Settlement.create(
                providerId,
                ProviderType.PERSONA_NATURAL,
                Money.of(new BigDecimal("50000.00")),
                Money.ZERO,
                null,
                null
        );

        when(settlementRepository.findByProviderIdAndPeriodStartAndPeriodEndAndSettlementType(eq(providerId), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        CreateSettlementCommand cmd = CreateSettlementCommand.builder()
                .providerId(providerId)
                .targetDate(targetDate)
                .build();

        Settlement result = service.execute(cmd);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Proveedor sin DailyBalances elegibles produce Settlement SKIPPED")
    void execute_NoEligibleBalances_ShouldReturnSkipped() {
        when(dailyBalanceRepository.findByProviderIdAndBalanceDateBetween(eq(providerId), any(), any()))
                .thenReturn(Collections.emptyList());

        CreateSettlementCommand cmd = CreateSettlementCommand.builder()
                .providerId(providerId)
                .targetDate(targetDate)
                .build();

        Settlement result = service.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.SKIPPED);
        assertThat(result.getNetAmount()).isEqualTo(Money.ZERO);
    }
}

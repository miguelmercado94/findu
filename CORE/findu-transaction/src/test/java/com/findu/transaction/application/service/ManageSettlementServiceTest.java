package com.findu.transaction.application.service;

import com.findu.transaction.application.port.in.ExecuteAllSettlementsUseCase;
import com.findu.transaction.application.port.in.ExecuteSettlementUseCase;
import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.application.port.in.dto.SettlementBatchResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult;
import com.findu.transaction.application.port.in.dto.SettlementRunResult.Outcome;
import com.findu.transaction.application.port.out.persistence.SettlementRepository;
import com.findu.transaction.domain.enums.ProviderType;
import com.findu.transaction.domain.enums.SettlementStatus;
import com.findu.transaction.domain.model.settlement.Settlement;
import com.findu.transaction.domain.valueobject.Money;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementExecutionResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ManageSettlementService ahora solo delega; la logica se prueba en SettlementExecutionServiceTest. */
@ExtendWith(MockitoExtension.class)
class ManageSettlementServiceTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private ExecuteSettlementUseCase executeSettlementUseCase;
    @Mock private ExecuteAllSettlementsUseCase executeAllSettlementsUseCase;

    private ManageSettlementService service;
    private final Long providerId = 1L;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:23:00Z"), ZoneId.of("America/Bogota"));
        service = new ManageSettlementService(settlementRepository, executeSettlementUseCase, executeAllSettlementsUseCase, clock);
    }

    private SettlementRunResult settled() {
        return SettlementRunResult.builder().providerId(providerId).outcome(Outcome.SETTLED)
                .settlementId("S-1").settlementCode("SET-1-20261108")
                .grossAmount(new BigDecimal("186300")).debtCompensationAmount(new BigDecimal("1300"))
                .netAmount(new BigDecimal("185000")).transferred(true).reference("MOCK-TRANSFER-x").build();
    }

    @Test
    @DisplayName("execute delega con el corte derivado de targetDate (domingo anterior) y devuelve la liquidacion")
    void executeDelegates() {
        Settlement stored = Settlement.create(providerId, ProviderType.PERSONA_NATURAL,
                Money.of(186300), Money.of(1300), null, null);
        when(executeSettlementUseCase.executeForProvider(providerId, LocalDate.of(2026, 11, 8))).thenReturn(settled());
        when(settlementRepository.findById("S-1")).thenReturn(Optional.of(stored));

        Settlement result = service.execute(CreateSettlementCommand.builder()
                .providerId(providerId).targetDate(LocalDate.of(2026, 11, 10)).build());

        assertThat(result).isSameAs(stored);
    }

    @Test
    @DisplayName("Nada que liquidar sin liquidaciones previas => SKIPPED en memoria, sin persistir")
    void nothingToSettleReturnsSkipped() {
        when(executeSettlementUseCase.executeForProvider(providerId, null)).thenReturn(
                SettlementRunResult.builder().providerId(providerId).outcome(Outcome.NOTHING_TO_SETTLE).build());
        when(settlementRepository.findLatestCompletedByProviderId(providerId)).thenReturn(Optional.empty());

        Settlement result = service.execute(CreateSettlementCommand.builder().providerId(providerId).build());

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.SKIPPED);
        assertThat(result.getNetAmount()).isEqualTo(Money.ZERO);
        org.mockito.Mockito.verify(settlementRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("executeManual con providerId liquida solo a ese proveedor y resume bruto/compensacion/neto/referencia")
    void manualSingleProvider() {
        LocalDate cutoff = LocalDate.of(2026, 11, 8);
        when(executeSettlementUseCase.executeForProvider(providerId, cutoff)).thenReturn(settled());

        SettlementExecutionResultResponse res = service.executeManual(providerId, null);

        assertThat(res.getCutoffDate()).isEqualTo(cutoff);
        assertThat(res.getNextSettlementDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(res.getSettlementsCreated()).isEqualTo(1);
        assertThat(res.getStatus()).isEqualTo("COMPLETED");
        SettlementExecutionResultResponse.ProviderResult r = res.getResults().get(0);
        assertThat(r.getSettlementCode()).isEqualTo("SET-1-20261108");
        assertThat(r.getGrossAmount()).isEqualByComparingTo("186300");
        assertThat(r.getDebtCompensationAmount()).isEqualByComparingTo("1300");
        assertThat(r.getNetAmount()).isEqualByComparingTo("185000");
        assertThat(r.getReference()).startsWith("MOCK-TRANSFER-");
    }

    @Test
    @DisplayName("executeManual sin providerId liquida a todos y cuenta omitidos/fallidos")
    void manualAll() {
        LocalDate cutoff = LocalDate.of(2026, 11, 8);
        when(executeAllSettlementsUseCase.executeAll(null)).thenReturn(SettlementBatchResult.builder()
                .cutoffDate(cutoff)
                .results(List.of(settled(),
                        SettlementRunResult.builder().providerId(2L).outcome(Outcome.ALREADY_SETTLED).build(),
                        SettlementRunResult.builder().providerId(3L).outcome(Outcome.FAILED).message("boom").build()))
                .build());

        SettlementExecutionResultResponse res = service.executeManual(null, null);

        assertThat(res.getProvidersProcessed()).isEqualTo(3);
        assertThat(res.getSettlementsCreated()).isEqualTo(1);
        assertThat(res.getSettlementsSkipped()).isEqualTo(1);
        assertThat(res.getProvidersFailed()).isEqualTo(1);
        assertThat(res.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(res.getErrors()).hasSize(1);
        verify(executeAllSettlementsUseCase).executeAll(null);
    }
}

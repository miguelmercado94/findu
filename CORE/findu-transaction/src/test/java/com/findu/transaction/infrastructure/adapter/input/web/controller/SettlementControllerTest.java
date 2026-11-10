package com.findu.transaction.infrastructure.adapter.input.web.controller;

import com.findu.transaction.application.port.in.CreateSettlementUseCase;
import com.findu.transaction.application.port.in.GetSettlementUseCase;
import com.findu.transaction.application.service.ManageSettlementService;
import com.findu.transaction.infrastructure.adapter.input.web.dto.response.SettlementExecutionResultResponse;
import com.findu.transaction.infrastructure.adapter.input.web.mapper.TransactionWebMapper;
import com.findu.transaction.infrastructure.adapter.input.web.security.InternalServiceKeyGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    private static final String KEY = "test-internal-key";

    @Mock private CreateSettlementUseCase createSettlementUseCase;
    @Mock private GetSettlementUseCase getSettlementUseCase;
    @Mock private ManageSettlementService manageSettlementService;
    @Mock private TransactionWebMapper webMapper;
    private final InternalServiceKeyGuard guard = new InternalServiceKeyGuard();
    private SettlementController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(guard, "expectedKey", KEY);
        controller = new SettlementController(createSettlementUseCase, getSettlementUseCase,
                manageSettlementService, webMapper, guard);
    }

    private SettlementExecutionResultResponse example() {
        return SettlementExecutionResultResponse.builder()
                .status("COMPLETED").cutoffDate(LocalDate.of(2026, 11, 8))
                .results(List.of(SettlementExecutionResultResponse.ProviderResult.builder()
                        .providerId(1L).outcome("SETTLED").settlementCode("SET-1-20261108")
                        .grossAmount(new BigDecimal("186300")).debtCompensationAmount(new BigDecimal("1300"))
                        .netAmount(new BigDecimal("185000")).reference("MOCK-TRANSFER-x").transferred(true).build()))
                .build();
    }

    @Test
    @DisplayName("Con llave correcta: 200 y resumen con neto y referencia")
    void executeWithKey() {
        when(manageSettlementService.executeManual(1L, null)).thenReturn(example());

        ResponseEntity<SettlementExecutionResultResponse> res = controller.executeSettlement(KEY, 1L, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getResults().get(0).getNetAmount()).isEqualByComparingTo("185000");
        assertThat(res.getBody().getResults().get(0).getReference()).startsWith("MOCK-TRANSFER-");
    }

    @Test
    @DisplayName("Sin llave o llave incorrecta: 403 y no se liquida")
    void executeWithoutOrWrongKey() {
        assertThat(controller.executeSettlement(null, 1L, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.executeSettlement("otra", 1L, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(manageSettlementService);
    }

    @Test
    @DisplayName("Llave del servidor vacia => fail-closed (403 aun con header)")
    void emptyServerKeyIsFailClosed() {
        ReflectionTestUtils.setField(guard, "expectedKey", "");
        assertThat(controller.executeSettlement("", 1L, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(manageSettlementService);
    }

    @Test
    @DisplayName("providerId omitido liquida a todos (el servicio recibe null)")
    void executeAll() {
        when(manageSettlementService.executeManual(null, null)).thenReturn(example());
        assertThat(controller.executeSettlement(KEY, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(manageSettlementService).executeManual(null, null);
    }

    @Test
    @DisplayName("Pasarela no configurada => 503")
    void gatewayMissing() {
        when(manageSettlementService.executeManual(any(), any())).thenThrow(new IllegalStateException("sin pasarela"));
        assertThat(controller.executeSettlement(KEY, 1L, null).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Los endpoints legacy /run y /provider/{id} tambien exigen la llave")
    void legacyEndpointsGuarded() {
        assertThat(controller.runBatchSettlements(null, null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.runProviderSettlement(null, 1L, null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(manageSettlementService, never()).runBatchSettlements(any(), any());
        verify(createSettlementUseCase, never()).execute(any());
    }
}
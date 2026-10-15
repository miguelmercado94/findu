package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.response.ActiveAlertDto;
import com.findu.validacion.application.port.in.GetActiveAlertsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationAlertControllerTest {

    @Mock
    private GetActiveAlertsUseCase getActiveAlertsUseCase;

    @InjectMocks
    private ValidationAlertController controller;

    @Test
    @DisplayName("GET /alerts con entityId retorna 200 OK y la lista de alertas")
    void getAlerts_shouldReturnOkWithAlerts() {
        ActiveAlertDto alert1 = new ActiveAlertDto(
                "uuid-1", "PERFIL_CORREO_PENDIENTE", "EMAIL_VERIFICATION", "PERFIL_CORREO_PENDIENTE",
                true, "INFO", "Confirma tu correo", "Body correo", null, null, true, 1, "home");
        ActiveAlertDto alert2 = new ActiveAlertDto(
                "uuid-2", "PERFIL_BIOMETRIA_PENDIENTE", "BIOMETRIC_FACIAL", "PERFIL_BIOMETRIA_PENDIENTE",
                true, "INFO", "Validación biométrica", "Body bio", "Validar ahora", "findu://perfil/verificacion-biometrica", true, 2, "home");

        when(getActiveAlertsUseCase.getActiveAlerts("USER", "42"))
                .thenReturn(Mono.just(List.of(alert1, alert2)));

        Mono<ResponseEntity<List<ActiveAlertDto>>> result = controller.getAlerts("USER", "42", null);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody()).hasSize(2);
                    assertThat(res.getBody().get(0).codigo()).isEqualTo("PERFIL_CORREO_PENDIENTE");
                    assertThat(res.getBody().get(1).codigo()).isEqualTo("PERFIL_BIOMETRIA_PENDIENTE");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /alerts con userId numérico lo resuelve como entityId")
    void getAlerts_shouldUseUserIdWhenEntityIdNull() {
        when(getActiveAlertsUseCase.getActiveAlerts("USER", "99"))
                .thenReturn(Mono.just(List.of()));

        Mono<ResponseEntity<List<ActiveAlertDto>>> result = controller.getAlerts("USER", null, 99L);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /alerts sin entityId ni userId retorna lista vacía 200")
    void getAlerts_shouldReturnEmptyWhenNoIdProvided() {
        Mono<ResponseEntity<List<ActiveAlertDto>>> result = controller.getAlerts("USER", null, null);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody()).isEmpty();
                })
                .verifyComplete();
    }
}

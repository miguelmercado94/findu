package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.CheckActiveRequest;
import com.findu.validacion.application.dto.request.CompleteProcessRequest;
import com.findu.validacion.application.dto.request.InspectRequest;
import com.findu.validacion.application.dto.response.ActiveValidationDto;
import com.findu.validacion.application.dto.response.CheckActiveResponse;
import com.findu.validacion.application.dto.response.CompleteProcessResponse;
import com.findu.validacion.application.dto.response.InspectResponse;
import com.findu.validacion.application.dto.response.PassiveValidationDto;
import com.findu.validacion.application.port.in.CheckActiveValidationsUseCase;
import com.findu.validacion.application.port.in.CompleteProcessUseCase;
import com.findu.validacion.application.port.in.InspectEndpointUseCase;
import com.findu.validacion.domain.enums.ExecutionType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {

    @Mock
    private InspectEndpointUseCase inspectUseCase;

    @Mock
    private CheckActiveValidationsUseCase checkActiveUseCase;

    @Mock
    private CompleteProcessUseCase completeProcessUseCase;

    @InjectMocks
    private ValidationController validationController;

    @Test
    @DisplayName("POST /inspect retorna 200 OK con InspectResponse")
    void inspect_shouldReturnOk() {
        InspectRequest request = new InspectRequest("findu-spring-security", "POST", "/api/v1/registration/onboarding");
        InspectResponse response = new InspectResponse(
                "ONBOARDING_PROVEEDOR",
                List.of(new ActiveValidationDto("OTP_PHONE_SMS", true, "/api/v1/auth/phone/verify-otp")),
                List.of(new PassiveValidationDto("EMAIL_VERIFICATION", "PERFIL_CORREO_PENDIENTE", ExecutionType.CLIENT_ACTION, "/api/v1/auth/email/verify"))
        );

        when(inspectUseCase.inspect(any(InspectRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<InspectResponse>> result = validationController.inspect(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().processCode()).isEqualTo("ONBOARDING_PROVEEDOR");
                    assertThat(res.getBody().active()).hasSize(1);
                    assertThat(res.getBody().passive()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /check-active retorna 200 OK con CheckActiveResponse")
    void checkActive_shouldReturnOk() {
        CheckActiveRequest request = new CheckActiveRequest("ONBOARDING_PROVEEDOR", "2", List.of());
        CheckActiveResponse response = new CheckActiveResponse(true, List.of());

        when(checkActiveUseCase.check(any(CheckActiveRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<CheckActiveResponse>> result = validationController.checkActive(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().allowed()).isTrue();
                    assertThat(res.getBody().failed()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("POST /complete-process retorna 200 OK con CompleteProcessResponse")
    void completeProcess_shouldReturnOk() {
        CompleteProcessRequest request = new CompleteProcessRequest(
                "ONBOARDING_PROVEEDOR", "2", 2L, "USER", List.of(), List.of());
        CompleteProcessResponse response = new CompleteProcessResponse(
                "ONBOARDING_PROVEEDOR", List.of("EMAIL_VERIFICATION"), 1);

        when(completeProcessUseCase.complete(any(CompleteProcessRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<CompleteProcessResponse>> result = validationController.completeProcess(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().processCode()).isEqualTo("ONBOARDING_PROVEEDOR");
                    assertThat(res.getBody().alertsEmitted()).isEqualTo(1);
                })
                .verifyComplete();
    }
}

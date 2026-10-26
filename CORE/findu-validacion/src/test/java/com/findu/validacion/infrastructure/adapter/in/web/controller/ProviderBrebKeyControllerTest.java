package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.RegisterBrebKeyRequest;
import com.findu.validacion.application.dto.request.UpdateBrebKeyRequest;
import com.findu.validacion.application.dto.response.ProviderBrebKeyResponse;
import com.findu.validacion.application.port.in.ManageProviderBrebKeyUseCase;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderBrebKeyControllerTest {

    @Mock
    private ManageProviderBrebKeyUseCase manageBrebKeyUseCase;

    @InjectMocks
    private ProviderBrebKeyController controller;

    @Test
    @DisplayName("POST /api/v1/validations/bre-b-keys retorna 201 CREATED con ProviderBrebKeyResponse")
    void registerKey_shouldReturnCreated() {
        RegisterBrebKeyRequest request = new RegisterBrebKeyRequest(
                2L, "CELULAR", "+57 300 376 3300", "Bancolombia", "3003763300", "+57", "123456");

        ProviderBrebKeyResponse response = new ProviderBrebKeyResponse(
                "key-1", 2L, "CELULAR", "+57 300 376 3300", "+57 300 *** **00",
                "Bancolombia", "3003763300", "ACTIVE", Instant.now(), Instant.now());

        when(manageBrebKeyUseCase.registerKey(any(RegisterBrebKeyRequest.class)))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<ProviderBrebKeyResponse>> result = controller.registerKey(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().bank()).isEqualTo("Bancolombia");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("PUT /api/v1/validations/bre-b-keys/{providerId} retorna 200 OK")
    void updateKey_shouldReturnOk() {
        UpdateBrebKeyRequest request = new UpdateBrebKeyRequest(
                "CELULAR", "+57 311 000 1122", "Nequi", "3110001122", "+57", "123456");

        ProviderBrebKeyResponse response = new ProviderBrebKeyResponse(
                "key-1", 2L, "CELULAR", "+57 311 000 1122", "+57 311 *** **22",
                "Nequi", "3110001122", "ACTIVE", Instant.now(), Instant.now());

        when(manageBrebKeyUseCase.updateKey(eq(2L), any(UpdateBrebKeyRequest.class)))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<ProviderBrebKeyResponse>> result = controller.updateKey(2L, request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().bank()).isEqualTo("Nequi");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /api/v1/validations/bre-b-keys/{providerId} retorna 200 OK")
    void getKey_shouldReturnOk() {
        ProviderBrebKeyResponse response = new ProviderBrebKeyResponse(
                "key-1", 2L, "CELULAR", "+57 300 376 3300", "+57 300 *** **00",
                "Bancolombia", "3003763300", "ACTIVE", Instant.now(), Instant.now());

        when(manageBrebKeyUseCase.getKey(2L)).thenReturn(Mono.just(response));

        Mono<ResponseEntity<ProviderBrebKeyResponse>> result = controller.getKey(2L);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().providerId()).isEqualTo(2L);
                })
                .verifyComplete();
    }
}

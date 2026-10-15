package com.findu.validacion.infrastructure.adapter.in.web.controller;

import com.findu.validacion.application.dto.request.SupportResolveRequest;
import com.findu.validacion.application.dto.response.SupportResolveResponse;
import com.findu.validacion.application.port.in.SupportResolveUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    @Mock
    private SupportResolveUseCase supportResolveUseCase;

    @InjectMocks
    private SupportController supportController;

    @Test
    @DisplayName("POST /resolve retorna 200 OK con SupportResolveResponse")
    void resolve_shouldReturnOk() {
        SupportResolveRequest request = new SupportResolveRequest(
                "ONBOARDING_PROVEEDOR", "2", "USER", "JUDICIAL_BACKGROUND", "PASSED", 1L, "OK");
        SupportResolveResponse response = new SupportResolveResponse("PASSED", "uuid-xyz");

        when(supportResolveUseCase.resolve(any(SupportResolveRequest.class))).thenReturn(Mono.just(response));

        Mono<ResponseEntity<SupportResolveResponse>> result = supportController.resolve(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(res.getBody()).isNotNull();
                    assertThat(res.getBody().status()).isEqualTo("PASSED");
                    assertThat(res.getBody().alertUuid()).isEqualTo("uuid-xyz");
                })
                .verifyComplete();
    }
}

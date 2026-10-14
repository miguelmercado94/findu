package com.findu.gateway.service;

import com.findu.gateway.controller.OnboardingController;
import com.findu.gateway.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private SagaPendingCompensationStore pendingCompensationStore;

    @Mock
    private PhoneOtpValidationService otpValidationService;

    @Mock
    private IdempotencyService idempotencyService;

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    }

    private OnboardingRequest sampleRequest(String flowType) {
        return new OnboardingRequest(
                flowType,
                false,
                null,
                new SecurityData("provider1", "prov@findu.com", "pass123", "3003763300", "+57", true),
                new PersonaData("Provider One", "CC", "114000111", "1994-05-20", "MASCULINO"),
                new DireccionData(149L, "Cra 1 # 2-3", 10.98, -74.80),
                new CoberturaData(List.of(149L, 150L)),
                List.of(new EspecialidadItem(12L, 5)),
                new BiometriaData(true, false, 0.0, null, null, null),
                "idemp-key-123"
        );
    }

    private ClientResponse jsonResponse(HttpStatus status, String jsonBody) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(jsonBody)
                .build();
    }

    @Test
    void executeSaga_happyPath_success() {
        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();
            HttpMethod method = request.method();

            if (method == HttpMethod.POST && path.endsWith("/internal/users")) {
                return Mono.just(jsonResponse(HttpStatus.CREATED, "{\"userId\": 10}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/internal/perfiles")) {
                return Mono.just(jsonResponse(HttpStatus.CREATED, "{\"perfilId\": 20}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/cobertura")) {
                return Mono.just(jsonResponse(HttpStatus.OK, "{}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/especialidades")) {
                return Mono.just(jsonResponse(HttpStatus.OK, "{}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/internal/activation/init")) {
                return Mono.just(jsonResponse(HttpStatus.OK, "{\"estadoActivacion\": \"PENDIENTE_VALIDACIONES\"}"));
            }
            if (method == HttpMethod.POST && path.contains("/tokens")) {
                return Mono.just(jsonResponse(HttpStatus.OK, "{\"accessToken\": \"jwt_acc\", \"refreshToken\": \"jwt_ref\", \"tokenType\": \"Bearer\"}"));
            }
            return Mono.just(jsonResponse(HttpStatus.NOT_FOUND, "{}"));
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        when(otpValidationService.consumePhoneVerified(any(), any())).thenReturn(Mono.empty());

        SagaOrchestratorService orchestrator = new SagaOrchestratorService(
                builder, pendingCompensationStore, otpValidationService, circuitBreakerRegistry);

        OnboardingRequest request = sampleRequest("PROVEEDOR");

        StepVerifier.create(orchestrator.executeSaga(request, "req-1", "idemp-key-123"))
                .assertNext(response -> {
                    assertThat(response.userId()).isEqualTo(10L);
                    assertThat(response.perfilId()).isEqualTo(20L);
                    assertThat(response.estadoActivacion()).isEqualTo("PENDIENTE_VALIDACIONES");
                    assertThat(response.tokens().accessToken()).isEqualTo("jwt_acc");
                })
                .verifyComplete();
    }

    @Test
    void executeSaga_failureInT2_triggersC1Compensation_success() {
        AtomicInteger deleteUserCalls = new AtomicInteger(0);

        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();
            HttpMethod method = request.method();

            if (method == HttpMethod.POST && path.endsWith("/internal/users")) {
                return Mono.just(jsonResponse(HttpStatus.CREATED, "{\"userId\": 10}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/internal/perfiles")) {
                // T2 Fails with 500 Internal Server Error
                return Mono.just(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"DB_DOWN\"}"));
            }
            if (method == HttpMethod.DELETE && path.endsWith("/internal/users/10")) {
                deleteUserCalls.incrementAndGet();
                return Mono.just(jsonResponse(HttpStatus.NO_CONTENT, ""));
            }
            return Mono.just(jsonResponse(HttpStatus.NOT_FOUND, "{}"));
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);

        SagaOrchestratorService orchestrator = new SagaOrchestratorService(
                builder, pendingCompensationStore, otpValidationService, circuitBreakerRegistry);

        OnboardingRequest request = sampleRequest("PROVEEDOR");

        StepVerifier.create(orchestrator.executeSaga(request, "req-fail-t2", "idemp-1"))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(SagaOrchestratorService.SagaException.class);
                    SagaOrchestratorService.SagaException ex = (SagaOrchestratorService.SagaException) throwable;
                    assertThat(ex.getErrorResponse().step()).isEqualTo("CORE_CREATE_PERFIL");
                    assertThat(ex.getErrorResponse().compensated()).isTrue();
                })
                .verify();

        assertThat(deleteUserCalls.get()).isEqualTo(1);
        verifyNoInteractions(pendingCompensationStore);
    }

    @Test
    void executeSaga_failureInT2_whenC1Fails_savedToOutboxStore() {
        AtomicInteger deleteUserAttempts = new AtomicInteger(0);

        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();
            HttpMethod method = request.method();

            if (method == HttpMethod.POST && path.endsWith("/internal/users")) {
                return Mono.just(jsonResponse(HttpStatus.CREATED, "{\"userId\": 10}"));
            }
            if (method == HttpMethod.POST && path.endsWith("/internal/perfiles")) {
                return Mono.just(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"CORE_FAILED\"}"));
            }
            if (method == HttpMethod.DELETE && path.endsWith("/internal/users/10")) {
                deleteUserAttempts.incrementAndGet();
                // C1 fails with 500
                return Mono.just(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"SECURITY_DELETE_FAILED\"}"));
            }
            return Mono.just(jsonResponse(HttpStatus.NOT_FOUND, "{}"));
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        when(pendingCompensationStore.savePendingCompensation(any())).thenReturn(Mono.empty());

        SagaOrchestratorService orchestrator = new SagaOrchestratorService(
                builder, pendingCompensationStore, otpValidationService, circuitBreakerRegistry);

        OnboardingRequest request = sampleRequest("PROVEEDOR");

        StepVerifier.create(orchestrator.executeSaga(request, "req-outbox", "idemp-2"))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(SagaOrchestratorService.SagaException.class);
                    SagaOrchestratorService.SagaException ex = (SagaOrchestratorService.SagaException) throwable;
                    assertThat(ex.getErrorResponse().step()).isEqualTo("CORE_CREATE_PERFIL");
                    assertThat(ex.getErrorResponse().compensated()).isFalse();
                })
                .verify();

        assertThat(deleteUserAttempts.get()).isGreaterThanOrEqualTo(1);
        verify(pendingCompensationStore, times(1)).savePendingCompensation(argThat(comp ->
                "SECURITY".equals(comp.targetService()) &&
                "DELETE_USER".equals(comp.action()) &&
                Long.valueOf(10L).equals(comp.targetId())
        ));
    }

    @Test
    void idempotency_sameKeyReturnsCachedResponse() {
        OnboardingRequest request = sampleRequest("CLIENTE");
        OnboardingResponse cachedResponse = new OnboardingResponse(15L, 25L,
                new OnboardingResponse.TokensData("jwt_cached", "ref_cached", "Bearer"), "ACTIVO");

        when(idempotencyService.getExisting("idemp-key-123")).thenReturn(Mono.just(cachedResponse));

        SagaOrchestratorService mockOrchestrator = mock(SagaOrchestratorService.class);
        OnboardingController controller = new OnboardingController(
                mockOrchestrator, idempotencyService, otpValidationService);

        StepVerifier.create(controller.registerOnboarding("req-idemp", "idemp-key-123", request))
                .assertNext(responseEntity -> {
                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(responseEntity.getBody()).isEqualTo(cachedResponse);
                })
                .verifyComplete();

        verifyNoInteractions(mockOrchestrator);
    }
}

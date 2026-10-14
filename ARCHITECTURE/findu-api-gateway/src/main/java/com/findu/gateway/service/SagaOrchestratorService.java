package com.findu.gateway.service;

import com.findu.gateway.dto.OnboardingRequest;
import com.findu.gateway.dto.OnboardingResponse;
import com.findu.gateway.dto.PendingCompensation;
import com.findu.gateway.dto.SagaErrorResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SagaOrchestratorService {

    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final Duration STEP_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final SagaPendingCompensationStore pendingCompensationStore;
    private final PhoneOtpValidationService otpValidationService;
    private final CircuitBreaker securityCircuitBreaker;
    private final CircuitBreaker coreCircuitBreaker;

    @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}")
    private String internalServiceKey;

    @Value("${findu.services.security-url:http://localhost:8081}")
    private String securityUrl;

    @Value("${findu.services.core-url:http://localhost:8082}")
    private String coreUrl;

    public SagaOrchestratorService(WebClient.Builder webClientBuilder,
                                   SagaPendingCompensationStore pendingCompensationStore,
                                   PhoneOtpValidationService otpValidationService,
                                   CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClientBuilder.build();
        this.pendingCompensationStore = pendingCompensationStore;
        this.otpValidationService = otpValidationService;
        this.securityCircuitBreaker = circuitBreakerRegistry.circuitBreaker("securityService");
        this.coreCircuitBreaker = circuitBreakerRegistry.circuitBreaker("coreService");
    }

    public Mono<OnboardingResponse> executeSaga(OnboardingRequest request, String requestId, String idempotencyKey) {
        final String correlationId = (requestId != null && !requestId.isBlank()) ? requestId : UUID.randomUUID().toString();
        final String effectiveIdempotency = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey : request.idempotencyKey();

        boolean isCliente = "CLIENTE".equalsIgnoreCase(request.flowType());
        log.info("[Saga][{}] Starting onboarding saga for flowType={} user={}",
                correlationId, request.flowType(), request.security().username());

        SagaContext context = new SagaContext(correlationId, effectiveIdempotency, request);

        // T1: security.createUser
        return stepT1CreateUser(context)
                .onErrorResume(e -> {
                    log.error("[Saga][{}] T1 (security.createUser) failed: {}", context.correlationId, e.getMessage());
                    return failWithSagaError("SECURITY_CREATE_USER", true, e);
                })
                // T2: core.createPerfil
                .flatMap(ctx -> stepT2CreatePerfil(ctx)
                        .onErrorResume(e -> compensateAfterT2(ctx, e)))
                // T3: core.addCobertura (solo PROVEEDOR)
                .flatMap(ctx -> {
                    if (isCliente) return Mono.just(ctx);
                    return stepT3AddCobertura(ctx)
                            .onErrorResume(e -> compensateAfterT3(ctx, e));
                })
                // T4: core.addEspecialidades (solo PROVEEDOR)
                .flatMap(ctx -> {
                    if (isCliente) return Mono.just(ctx);
                    return stepT4AddEspecialidades(ctx)
                            .onErrorResume(e -> compensateAfterT4(ctx, e));
                })
                // T5: security.initActivation (solo PROVEEDOR)
                .flatMap(ctx -> {
                    if (isCliente) return Mono.just(ctx);
                    return stepT5InitActivation(ctx)
                            .onErrorResume(e -> compensateAfterT5(ctx, e));
                })
                // T6: security.issueTokens
                .flatMap(ctx -> stepT6IssueTokens(ctx)
                        .onErrorResume(e -> compensateAfterT6(ctx, e)))
                .map(this::buildResponse)
                .doOnSuccess(res -> {
                    log.info("[Saga][{}] Saga successfully completed for userId={} perfilId={}",
                            correlationId, res.userId(), res.perfilId());
                    // Consumir flag de verificación de teléfono tras submit exitoso
                    otpValidationService.consumePhoneVerified(
                            request.security().phone(),
                            request.security().codPhoneInternational()).subscribe();
                });
    }

    // --- PASO T1 ---
    private Mono<SagaContext> stepT1CreateUser(SagaContext ctx) {
        log.info("[Saga][{}] T1: Creating user in security", ctx.correlationId);
        String roleName = "CLIENTE".equalsIgnoreCase(ctx.request.flowType()) ? "ROLE_OUR_CLIENTE" : "ROLE_OUR_PROVEEDOR";

        Map<String, Object> body = new HashMap<>();
        body.put("username", ctx.request.security().username());
        body.put("email", ctx.request.security().email());
        body.put("password", ctx.request.security().password() != null ? ctx.request.security().password() : "");
        body.put("phone", ctx.request.security().phone());
        body.put("codPhoneInternational", ctx.request.security().codPhoneInternational() != null ? ctx.request.security().codPhoneInternational() : "+57");
        body.put("roleName", roleName);
        body.put("isFederated", Boolean.TRUE.equals(ctx.request.isFederated()));
        body.put("provider", ctx.request.federated() != null ? ctx.request.federated().provider() : null);
        body.put("providerUserId", ctx.request.federated() != null ? ctx.request.federated().providerUserId() : null);

        var reqSpec = webClient.post()
                .uri(securityUrl + "/internal/users")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId);

        if (ctx.idempotencyKey != null && !ctx.idempotencyKey.isBlank()) {
            reqSpec.header(HEADER_IDEMPOTENCY_KEY, ctx.idempotencyKey);
        }

        return reqSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(securityCircuitBreaker))
                .map(resp -> {
                    Long userId = ((Number) resp.get("userId")).longValue();
                    ctx.userId = userId;
                    log.info("[Saga][{}] T1 Success: userId={}", ctx.correlationId, userId);
                    return ctx;
                });
    }

    // --- PASO T2 ---
    private Mono<SagaContext> stepT2CreatePerfil(SagaContext ctx) {
        log.info("[Saga][{}] T2: Creating perfil in core for userId={}", ctx.correlationId, ctx.userId);

        Map<String, Object> body = Map.of(
                "authUserId", ctx.userId,
                "flowType", ctx.request.flowType(),
                "username", ctx.request.security().username(),
                "email", ctx.request.security().email(),
                "phone", ctx.request.security().phone(),
                "codPhoneInternational", ctx.request.security().codPhoneInternational() != null ? ctx.request.security().codPhoneInternational() : "+57",
                "persona", ctx.request.persona(),
                "direccion", ctx.request.direccion()
        );

        var reqSpec = webClient.post()
                .uri(coreUrl + "/internal/perfiles")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId);

        if (ctx.idempotencyKey != null && !ctx.idempotencyKey.isBlank()) {
            reqSpec.header(HEADER_IDEMPOTENCY_KEY, ctx.idempotencyKey);
        }

        return reqSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(coreCircuitBreaker))
                .map(resp -> {
                    Long perfilId = ((Number) resp.get("perfilId")).longValue();
                    ctx.perfilId = perfilId;
                    log.info("[Saga][{}] T2 Success: perfilId={}", ctx.correlationId, perfilId);
                    return ctx;
                });
    }

    // --- PASO T3 ---
    private Mono<SagaContext> stepT3AddCobertura(SagaContext ctx) {
        log.info("[Saga][{}] T3: Adding cobertura in core for perfilId={}", ctx.correlationId, ctx.perfilId);
        List<Long> munIds = ctx.request.cobertura() != null && ctx.request.cobertura().municipioIds() != null
                ? ctx.request.cobertura().municipioIds() : List.of();

        if (munIds.isEmpty()) {
            return Mono.just(ctx);
        }

        return webClient.post()
                .uri(coreUrl + "/internal/perfiles/" + ctx.perfilId + "/cobertura")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("municipioIds", munIds))
                .retrieve()
                .toBodilessEntity()
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(coreCircuitBreaker))
                .thenReturn(ctx);
    }

    // --- PASO T4 ---
    private Mono<SagaContext> stepT4AddEspecialidades(SagaContext ctx) {
        log.info("[Saga][{}] T4: Adding especialidades in core for perfilId={}", ctx.correlationId, ctx.perfilId);
        List<?> especialidades = ctx.request.especialidades() != null ? ctx.request.especialidades() : List.of();

        if (especialidades.isEmpty()) {
            return Mono.just(ctx);
        }

        return webClient.post()
                .uri(coreUrl + "/internal/perfiles/" + ctx.perfilId + "/especialidades")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(especialidades)
                .retrieve()
                .toBodilessEntity()
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(coreCircuitBreaker))
                .thenReturn(ctx);
    }

    // --- PASO T5 ---
    private Mono<SagaContext> stepT5InitActivation(SagaContext ctx) {
        log.info("[Saga][{}] T5: Initializing activation in security for userId={}", ctx.correlationId, ctx.userId);
        var bio = ctx.request.biometria();

        Map<String, Object> body = Map.of(
                "userId", ctx.userId,
                "biometriaIntentada", bio != null && Boolean.TRUE.equals(bio.intentada()),
                "biometriaAprobada", bio != null && Boolean.TRUE.equals(bio.aprobada()),
                "faceMatchScore", bio != null && bio.faceMatchScore() != null ? bio.faceMatchScore() : 0.0,
                "fotoFrontalUrl", bio != null && bio.fotoFrontalUrl() != null ? bio.fotoFrontalUrl() : "",
                "cedulaNombre", bio != null && bio.cedulaNombre() != null ? bio.cedulaNombre() : "",
                "cedulaFechaNacimiento", bio != null && bio.cedulaFechaNacimiento() != null ? bio.cedulaFechaNacimiento() : ""
        );

        return webClient.post()
                .uri(securityUrl + "/internal/activation/init")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(securityCircuitBreaker))
                .map(resp -> {
                    ctx.estadoActivacion = (String) resp.get("estadoActivacion");
                    return ctx;
                });
    }

    // --- PASO T6 ---
    private Mono<SagaContext> stepT6IssueTokens(SagaContext ctx) {
        log.info("[Saga][{}] T6: Issuing tokens in security for userId={}", ctx.correlationId, ctx.userId);

        return webClient.post()
                .uri(securityUrl + "/internal/users/" + ctx.userId + "/tokens")
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, ctx.correlationId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(STEP_TIMEOUT)
                .transformDeferred(CircuitBreakerOperator.of(securityCircuitBreaker))
                .map(resp -> {
                    ctx.tokens = new OnboardingResponse.TokensData(
                            (String) resp.get("accessToken"),
                            (String) resp.get("refreshToken"),
                            (String) resp.get("tokenType")
                    );
                    return ctx;
                });
    }

    // --- COMPENSACIONES ---

    private <T> Mono<T> compensateAfterT2(SagaContext ctx, Throwable error) {
        log.error("[Saga][{}] T2 (core.createPerfil) failed: {}. Triggering C1 compensation", ctx.correlationId, error.getMessage());
        return compensateC1(ctx.userId, ctx.correlationId)
                .flatMap(c1Ok -> failWithSagaError("CORE_CREATE_PERFIL", c1Ok, error));
    }

    private <T> Mono<T> compensateAfterT3(SagaContext ctx, Throwable error) {
        log.error("[Saga][{}] T3 (core.cobertura) failed: {}. Triggering C2 then C1 compensation", ctx.correlationId, error.getMessage());
        return compensateC2(ctx.perfilId, ctx.correlationId)
                .flatMap(c2Ok -> compensateC1(ctx.userId, ctx.correlationId).map(c1Ok -> c2Ok && c1Ok))
                .flatMap(allOk -> failWithSagaError("CORE_ADD_COBERTURA", allOk, error));
    }

    private <T> Mono<T> compensateAfterT4(SagaContext ctx, Throwable error) {
        log.error("[Saga][{}] T4 (core.especialidades) failed: {}. Triggering C2 then C1 compensation", ctx.correlationId, error.getMessage());
        return compensateC2(ctx.perfilId, ctx.correlationId)
                .flatMap(c2Ok -> compensateC1(ctx.userId, ctx.correlationId).map(c1Ok -> c2Ok && c1Ok))
                .flatMap(allOk -> failWithSagaError("CORE_ADD_ESPECIALIDADES", allOk, error));
    }

    private <T> Mono<T> compensateAfterT5(SagaContext ctx, Throwable error) {
        log.error("[Saga][{}] T5 (security.activation) failed: {}. Triggering C2 then C1 compensation", ctx.correlationId, error.getMessage());
        return compensateC2(ctx.perfilId, ctx.correlationId)
                .flatMap(c2Ok -> compensateC1(ctx.userId, ctx.correlationId).map(c1Ok -> c2Ok && c1Ok))
                .flatMap(allOk -> failWithSagaError("SECURITY_ACTIVATION_INIT", allOk, error));
    }

    private <T> Mono<T> compensateAfterT6(SagaContext ctx, Throwable error) {
        log.error("[Saga][{}] T6 (security.tokens) failed: {}. Triggering C2 then C1 compensation", ctx.correlationId, error.getMessage());
        return compensateC2(ctx.perfilId, ctx.correlationId)
                .flatMap(c2Ok -> compensateC1(ctx.userId, ctx.correlationId).map(c1Ok -> c2Ok && c1Ok))
                .flatMap(allOk -> failWithSagaError("SECURITY_ISSUE_TOKENS", allOk, error));
    }

    /**
     * Compensación C1: Elimina el usuario en security (idempotente). Con hasta 3 reintentos y outbox.
     */
    public Mono<Boolean> compensateC1(Long userId, String correlationId) {
        if (userId == null) {
            return Mono.just(true);
        }
        log.info("[Saga][{}] Executing C1 (security.deleteUser userId={})", correlationId, userId);

        return webClient.delete()
                .uri(securityUrl + "/internal/users/" + userId)
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, correlationId)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("[Saga][{}] C1 failed after retries for userId={}: {}. Queuing in outbox",
                            correlationId, userId, e.getMessage());
                    return pendingCompensationStore.savePendingCompensation(
                            PendingCompensation.of("SECURITY", "DELETE_USER", userId)
                    ).thenReturn(false);
                });
    }

    /**
     * Compensación C2: Elimina el perfil en core en cascada (idempotente). Con hasta 3 reintentos y outbox.
     */
    public Mono<Boolean> compensateC2(Long perfilId, String correlationId) {
        if (perfilId == null) {
            return Mono.just(true);
        }
        log.info("[Saga][{}] Executing C2 (core.deletePerfil perfilId={})", correlationId, perfilId);

        return webClient.delete()
                .uri(coreUrl + "/internal/perfiles/" + perfilId)
                .header(HEADER_INTERNAL_KEY, internalServiceKey)
                .header(HEADER_REQUEST_ID, correlationId)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("[Saga][{}] C2 failed after retries for perfilId={}: {}. Queuing in outbox",
                            correlationId, perfilId, e.getMessage());
                    return pendingCompensationStore.savePendingCompensation(
                            PendingCompensation.of("CORE", "DELETE_PERFIL", perfilId)
                    ).thenReturn(false);
                });
    }

    private <T> Mono<T> failWithSagaError(String step, boolean compensated, Throwable cause) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String message = "No se pudo completar el registro. Intenta de nuevo.";

        if (cause instanceof WebClientResponseException wcre) {
            if (wcre.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                status = HttpStatus.CONFLICT;
                message = "Ya existe una cuenta con estos datos, inicia sesión";
            } else if (wcre.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                status = HttpStatus.BAD_REQUEST;
                message = "Datos de registro inválidos. Verifica la información ingresada.";
            }
        }

        SagaErrorResponse err = new SagaErrorResponse(
                "REGISTRATION_FAILED",
                step,
                compensated,
                message
        );
        return Mono.error(new SagaException(status, err, cause));
    }

    private OnboardingResponse buildResponse(SagaContext ctx) {
        String estadoActivacion = ctx.estadoActivacion != null
                ? ctx.estadoActivacion
                : ("CLIENTE".equalsIgnoreCase(ctx.request.flowType()) ? "ACTIVO" : "PENDIENTE_VALIDACIONES");

        return new OnboardingResponse(
                ctx.userId,
                ctx.perfilId,
                ctx.tokens,
                estadoActivacion
        );
    }

    public static class SagaException extends ResponseStatusException {
        private final SagaErrorResponse errorResponse;

        public SagaException(HttpStatus status, SagaErrorResponse errorResponse, Throwable cause) {
            super(status, errorResponse.message(), cause);
            this.errorResponse = errorResponse;
        }

        public SagaErrorResponse getErrorResponse() {
            return errorResponse;
        }
    }

    private static class SagaContext {
        final String correlationId;
        final String idempotencyKey;
        final OnboardingRequest request;
        Long userId;
        Long perfilId;
        String estadoActivacion;
        OnboardingResponse.TokensData tokens;

        SagaContext(String correlationId, String idempotencyKey, OnboardingRequest request) {
            this.correlationId = correlationId;
            this.idempotencyKey = idempotencyKey;
            this.request = request;
        }
    }
}

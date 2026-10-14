package com.findu.gateway.controller;

import com.findu.gateway.dto.OnboardingRequest;
import com.findu.gateway.dto.OnboardingResponse;
import com.findu.gateway.dto.SagaErrorResponse;
import com.findu.gateway.service.IdempotencyService;
import com.findu.gateway.service.PhoneOtpValidationService;
import com.findu.gateway.service.SagaOrchestratorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/registration")
@Slf4j
public class OnboardingController {

    private final SagaOrchestratorService orchestratorService;
    private final IdempotencyService idempotencyService;
    private final PhoneOtpValidationService otpValidationService;

    public OnboardingController(SagaOrchestratorService orchestratorService,
                                IdempotencyService idempotencyService,
                                PhoneOtpValidationService otpValidationService) {
        this.orchestratorService = orchestratorService;
        this.idempotencyService = idempotencyService;
        this.otpValidationService = otpValidationService;
    }

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<Object>> registerOnboarding(
            @RequestHeader(name = "X-Request-Id", required = false) String requestId,
            @RequestHeader(name = "Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody OnboardingRequest request) {

        final String correlationId = (requestId != null && !requestId.isBlank()) ? requestId : UUID.randomUUID().toString();
        final String idempotencyKey = (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank())
                ? headerIdempotencyKey : request.idempotencyKey();

        // 1. Idempotencia: Verificar si ya existe resultado para esta clave
        return idempotencyService.getExisting(idempotencyKey)
                .map(cached -> {
                    log.info("[Onboarding][{}] Idempotency key '{}' hit cache, returning prior result",
                            correlationId, idempotencyKey);
                    return ResponseEntity.status(HttpStatus.CREATED).body((Object) cached);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 2. Revalidar OTP en Redis (no confiar en flag del cliente)
                    return otpValidationService.isPhoneVerified(
                            request.security().phone(),
                            request.security().codPhoneInternational()
                    ).flatMap(verified -> {
                        if (!Boolean.TRUE.equals(verified)) {
                            log.warn("[Onboarding][{}] Phone {} is not verified in Redis",
                                    correlationId, request.security().phone());
                            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) Map.of(
                                    "error", "PHONE_NOT_VERIFIED",
                                    "message", "El teléfono no ha sido verificado por OTP"
                            )));
                        }

                        // 3. Ejecutar saga orquestada
                        return orchestratorService.executeSaga(request, correlationId, idempotencyKey)
                                .flatMap(response -> idempotencyService.saveResult(idempotencyKey, response)
                                        .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body((Object) response)))
                                .onErrorResume(SagaOrchestratorService.SagaException.class, e -> {
                                    log.error("[Onboarding][{}] Saga execution failed: {}", correlationId, e.getMessage());
                                    return Mono.just(ResponseEntity.status(e.getStatusCode()).body(e.getErrorResponse()));
                                })
                                .onErrorResume(e -> {
                                    log.error("[Onboarding][{}] Unexpected error during saga: {}", correlationId, e.getMessage(), e);
                                    SagaErrorResponse err = new SagaErrorResponse(
                                            "INTERNAL_ERROR",
                                            "SAGA_UNKNOWN",
                                            false,
                                            "Error inesperado durante el registro: " + e.getMessage()
                                    );
                                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err));
                                });
                    });
                }));
    }
}

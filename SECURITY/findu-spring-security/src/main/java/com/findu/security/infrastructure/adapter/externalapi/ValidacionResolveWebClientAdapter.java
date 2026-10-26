package com.findu.security.infrastructure.adapter.externalapi;

import com.findu.security.application.port.output.externalapi.ValidacionResolvePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adapter WebClient hacia findu-validacion: resuelve como PASSED una validación del onboarding
 * del proveedor cuando se completa en security (correo verificado o biometría aprobada).
 * Operación best-effort: cualquier fallo (404, 5xx, timeout) se registra como warn y se ignora.
 */
@Component
public class ValidacionResolveWebClientAdapter implements ValidacionResolvePort {

    private static final Logger log = LoggerFactory.getLogger(ValidacionResolveWebClientAdapter.class);

    private static final String PROCESS_CODE = "ONBOARDING_PROVEEDOR";
    private static final String ENTITY_TYPE = "USER";
    private static final String RESOLUTION_PASSED = "PASSED";

    private final WebClient webClient;
    private final String resolvePath;

    public ValidacionResolveWebClientAdapter(
            @Value("${findu.validacion.base-url:http://findu-api-gateway:8080}") String baseUrl,
            @Value("${findu.validacion.resolve-path:/findu-validacion/api/v1/validations/support/resolve}") String resolvePath) {
        this.resolvePath = resolvePath;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    private record SupportResolveRequest(
            String processCode,
            String entityId,
            String entityType,
            String validationCode,
            String resolution,
            Long reviewerId,
            String nota
    ) {}

    @Override
    public Mono<Void> markPassed(Long userId, String validationCode, String nota) {
        SupportResolveRequest body = new SupportResolveRequest(
                PROCESS_CODE,
                String.valueOf(userId),
                ENTITY_TYPE,
                validationCode,
                RESOLUTION_PASSED,
                null,
                nota
        );
        return webClient.post()
                .uri(resolvePath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnSuccess(v -> log.info(
                        "Validación resuelta como PASSED en findu-validacion (userId={}, validationCode={}, nota={})",
                        userId, validationCode, nota))
                .onErrorResume(e -> {
                    log.warn("No se pudo marcar validación PASSED en findu-validacion (userId={}, validationCode={}, best-effort): {}",
                            userId, validationCode, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}

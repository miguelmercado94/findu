package com.findu.notification.processor.infrastructure.adapter.out.externalapi;

import com.findu.notification.processor.application.port.out.externalapi.SecurityUserLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter WebClient hacia findu-spring-security para resolver el proveedorId por username.
 *
 * Llama GET {@code /internal/users/by-username/{username}} con header
 * {@code X-Internal-Service-Key}. Degrada a {@link Optional#empty()} ante 4xx/5xx/timeout
 * (log, no lanza) para que el registro del token siga siendo best-effort.
 */
@Component
public class SecurityUserLookupAdapter implements SecurityUserLookupPort {

    private static final Logger log = LoggerFactory.getLogger(SecurityUserLookupAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String internalServiceKey;

    public SecurityUserLookupAdapter(
            @Value("${findu.services.security-url:http://localhost:8081/security-auth}") String securityUrl,
            @Value("${findu.internal.service-key:findu-internal-saga-secret-key-2026}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
        this.webClient = WebClient.builder().baseUrl(securityUrl).build();
    }

    /** Vista parcial de la respuesta de security (solo lo que necesitamos). */
    private record InternalUserLookupView(Long authUserId, String username, Long proveedorId) {}

    @Override
    public Optional<Long> findProveedorIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            InternalUserLookupView view = webClient.get()
                    .uri("/internal/users/by-username/{username}", username)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> Mono.empty())
                    .bodyToMono(InternalUserLookupView.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> {
                        log.warn("Lookup de proveedorId por username falló (best-effort): {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (view == null || view.proveedorId() == null) {
                return Optional.empty();
            }
            return Optional.of(view.proveedorId());
        } catch (Exception e) {
            log.warn("Lookup de proveedorId por username falló (best-effort): {}", e.getMessage());
            return Optional.empty();
        }
    }
}

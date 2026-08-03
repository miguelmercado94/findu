package com.findu.security.infrastructure.adapter.notification;

import com.findu.security.application.port.output.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Adapter que llama a la Lambda de notificaciones via HTTP POST.
 * Fire-and-forget: no bloquea el flujo principal si falla.
 */
@Component
public class NotificationLambdaAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationLambdaAdapter.class);

    private final WebClient webClient;
    private final boolean enabled;

    public NotificationLambdaAdapter(
            @Value("${findu.notification.lambda-url:}") String lambdaUrl,
            @Value("${findu.notification.enabled:false}") boolean enabled) {
        this.enabled = enabled;
        this.webClient = WebClient.builder()
                .baseUrl(lambdaUrl != null && !lambdaUrl.isBlank() ? lambdaUrl : "http://localhost:9000")
                .build();
    }

    @Override
    public Mono<Void> send(String type, String recipient, String templateCode, String language, Map<String, String> params) {
        if (!enabled) {
            log.debug("[NOTIFICATION STUB] type={} recipient={} template={}", type, recipient, templateCode);
            return Mono.empty();
        }

        Map<String, Object> body = Map.of(
                "type", type,
                "recipient", recipient,
                "templateCode", templateCode,
                "language", language != null ? language : "es",
                "params", params != null ? params : Map.of()
        );

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(r -> log.info("Notification dispatched: type={} recipient={} template={}", type, recipient, templateCode))
                .doOnError(e -> log.warn("Notification failed (non-blocking): type={} recipient={} error={}", type, recipient, e.getMessage()))
                .onErrorResume(e -> Mono.empty()) // fire-and-forget
                .then();
    }
}

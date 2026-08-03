package com.findu.core.infrastructure.adapter.externalapi;

import com.findu.core.application.port.output.externalapi.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter que llama a la Lambda de notificaciones via HTTP POST.
 * Usa @Async para no bloquear el flujo principal.
 */
@Component
public class NotificationLambdaAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationLambdaAdapter.class);

    private final RestTemplate restTemplate;
    private final String lambdaUrl;
    private final boolean enabled;

    public NotificationLambdaAdapter(
            @Value("${findu.notification.lambda-url:http://localhost:9000}") String lambdaUrl,
            @Value("${findu.notification.enabled:false}") boolean enabled) {
        this.lambdaUrl = lambdaUrl;
        this.enabled = enabled;
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Async
    public void send(String type, String recipient, String templateCode, String language, Map<String, String> params) {
        if (!enabled) {
            log.debug("[NOTIFICATION STUB] type={} recipient={} template={}", type, recipient, templateCode);
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("type", type);
            body.put("recipient", recipient);
            body.put("templateCode", templateCode);
            body.put("language", language != null ? language : "es");
            body.put("params", params != null ? params : Map.of());

            restTemplate.postForObject(lambdaUrl, body, String.class);
            log.info("Notification dispatched: type={} recipient={} template={}", type, recipient, templateCode);
        } catch (Exception e) {
            log.warn("Notification failed (non-blocking): type={} recipient={} error={}", type, recipient, e.getMessage());
        }
    }
}

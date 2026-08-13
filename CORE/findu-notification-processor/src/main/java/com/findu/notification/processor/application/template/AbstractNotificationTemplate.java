package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;

/**
 * TEMPLATE METHOD PATTERN — Define el algoritmo de procesamiento.
 * Las subclases solo implementan los pasos específicos de cada canal.
 * Usa TemplateService para obtener y renderizar plantillas desde BD.
 */
public abstract class AbstractNotificationTemplate {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final TemplateService templateService;

    protected AbstractNotificationTemplate(TemplateService templateService) {
        this.templateService = templateService;
    }

    private static record SendingPayload(String recipient, String subject, String body, Map<String, String> metadata) {}

    /** Template Method — algoritmo reactivo fijo */
    public final Mono<NotificationResult> execute(NotificationMessage message) {
        return Mono.fromCallable(() -> {
            validate(message);
            String recipient = resolveRecipient(message);
            String subject = buildSubject(message);
            String body = buildBody(message);
            return new SendingPayload(recipient, subject, body, message.getParams());
        })
        .flatMap(payload -> sendWithRecursiveRetry(payload, message, 1))
        .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<NotificationResult> sendWithRecursiveRetry(SendingPayload payload, NotificationMessage message, int attempt) {
        return Mono.fromCallable(() -> doSend(payload.recipient(), payload.subject(), payload.body(), payload.metadata()))
                .flatMap(result -> {
                    result.setRetries(attempt);
                    if (result.isSuccess()) {
                        return Mono.just(result);
                    } else {
                        if (attempt >= 3) {
                            log.error("Notification failed after {} attempts: id={} error={}", 
                                    attempt, message.getNotificationId(), result.getError());
                            return Mono.just(result);
                        }
                        log.warn("Attempt {} failed for notification id={} (channel={}). Retrying recursively... Reason: {}",
                                attempt, message.getNotificationId(), getChannel().name(), result.getError());
                        return Mono.delay(java.time.Duration.ofSeconds(attempt * 2L))
                                .then(sendWithRecursiveRetry(payload, message, attempt + 1));
                    }
                })
                .onErrorResume(ex -> {
                    if (attempt >= 3) {
                        return Mono.just(NotificationResult.builder()
                                .notificationId(message.getNotificationId())
                                .channel(getChannel())
                                .success(false)
                                .error("Exception: " + ex.getMessage())
                                .retries(attempt)
                                .build());
                    }
                    log.warn("Attempt {} threw exception for notification id={} (channel={}). Retrying... Exception: {}",
                            attempt, message.getNotificationId(), getChannel().name(), ex.getMessage());
                    return Mono.delay(java.time.Duration.ofSeconds(attempt * 2L))
                            .then(sendWithRecursiveRetry(payload, message, attempt + 1));
                });
    }

    public abstract NotificationChannel getChannel();

    /** Paso común: validación */
    protected void validate(NotificationMessage message) {
        if (message.getRecipient() == null || message.getRecipient().isBlank()) {
            throw new IllegalArgumentException("Recipient es requerido");
        }
        if (message.getTemplateCode() == null || message.getTemplateCode().isBlank()) {
            throw new IllegalArgumentException("TemplateCode es requerido");
        }
    }

    /** Cada canal resuelve el destinatario */
    protected abstract String resolveRecipient(NotificationMessage message);

    /** Construye el asunto (desde BD o fallback) */
    protected String buildSubject(NotificationMessage message) {
        Optional<NotificationTemplate> tpl = templateService.findTemplate(
                message.getTemplateCode(), getChannel().name(), language(message));
        return tpl.map(NotificationTemplate::getSubject)
                .orElse("FINDU - " + message.getTemplateCode().replace("_", " "));
    }

    /** Construye el cuerpo renderizado (desde BD o fallback) */
    protected String buildBody(NotificationMessage message) {
        Optional<NotificationTemplate> tpl = templateService.findTemplate(
                message.getTemplateCode(), getChannel().name(), language(message));

        if (tpl.isPresent()) {
            return templateService.render(tpl.get(), message.getParams());
        }
        // Fallback: template genérico inline
        return "Notificación: " + message.getTemplateCode() + " para " + message.getRecipient();
    }

    /** Cada canal envía de forma diferente */
    protected abstract NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata);

    private String language(NotificationMessage message) {
        return message.getLanguage() != null ? message.getLanguage() : "es";
    }
}

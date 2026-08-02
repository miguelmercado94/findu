package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** Template Method — algoritmo fijo */
    public final NotificationResult execute(NotificationMessage message) {
        validate(message);
        String recipient = resolveRecipient(message);
        String subject = buildSubject(message);
        String body = buildBody(message);
        return doSend(recipient, subject, body, message.getParams());
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

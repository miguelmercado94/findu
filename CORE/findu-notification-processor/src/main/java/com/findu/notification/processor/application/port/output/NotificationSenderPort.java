package com.findu.notification.processor.application.port.output;

import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import java.util.Map;

/**
 * STRATEGY PATTERN — Interfaz que define la estrategia de envío.
 * Cada canal (EMAIL, SMS, PUSH, WHATSAPP) implementa su propia estrategia.
 */
public interface NotificationSenderPort {

    /** Retorna el canal que esta implementación maneja */
    NotificationChannel getChannel();

    /** Envía la notificación */
    NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata);
}

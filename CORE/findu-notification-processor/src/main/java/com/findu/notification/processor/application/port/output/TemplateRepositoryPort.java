package com.findu.notification.processor.application.port.output;

import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.Optional;

/**
 * Puerto para acceder a las plantillas de notificación.
 * No depende de DynamoDB — puede ser cualquier BD no relacional.
 */
public interface TemplateRepositoryPort {

    /**
     * Busca una plantilla por código, canal e idioma.
     * Composite key: templateCode + channel + language
     */
    Optional<NotificationTemplate> findByCodeAndChannelAndLanguage(String templateCode, String channel, String language);

    /**
     * Fallback: busca por código y canal con idioma por defecto (es).
     */
    Optional<NotificationTemplate> findByCodeAndChannel(String templateCode, String channel);
}

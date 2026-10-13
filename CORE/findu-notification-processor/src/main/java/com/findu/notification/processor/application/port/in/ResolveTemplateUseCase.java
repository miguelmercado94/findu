package com.findu.notification.processor.application.port.in;

import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.Optional;

/**
 * Caso de uso de resolución de plantillas (hot path de envío, diseño §10).
 * Resuelve la versión activa ({@code v#ACTIVE}) por código, tipo funcional e idioma,
 * con fallback a {@code es}.
 */
public interface ResolveTemplateUseCase {

    /**
     * Resuelve la plantilla activa para el código/tipo/idioma dados. Aplica el
     * fallback de idioma a {@code es} y devuelve vacío si no hay versión activa.
     */
    Optional<NotificationTemplate> resolveActive(String codigo, TemplateType tipo, String language);
}

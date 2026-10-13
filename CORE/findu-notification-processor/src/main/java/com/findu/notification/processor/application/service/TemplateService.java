package com.findu.notification.processor.application.service;

import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.Map;
import java.util.Optional;

public interface TemplateService {

    /**
     * Resuelve la plantilla activa por código, tipo funcional e idioma (hot path).
     * Aplica fallback de idioma a {@code es}.
     */
    Optional<NotificationTemplate> findTemplate(String templateCode, TemplateType tipo, String language);

    /**
     * Compatibilidad: resuelve por canal técnico (EMAIL/SMS/PUSH/WHATSAPP), mapeado
     * internamente a {@link TemplateType} (PUSH → NOTIFICACION).
     */
    Optional<NotificationTemplate> findTemplate(String templateCode, String channel, String language);

    /**
     * Renderiza la plantilla reemplazando {{key}} por los valores de params, validando
     * previamente que las variables declaradas estén presentes.
     * Si contentType=HTML, el body ya contiene el HTML completo (solo se reemplazan variables).
     * Si contentType=TXT, retorna texto plano.
     */
    String render(NotificationTemplate template, Map<String, String> params);
}

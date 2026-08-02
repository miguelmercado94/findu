package com.findu.notification.processor.application.service;

import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.Map;
import java.util.Optional;

public interface TemplateService {

    Optional<NotificationTemplate> findTemplate(String templateCode, String channel, String language);

    /**
     * Renderiza la plantilla reemplazando {{key}} por los valores de params.
     * Si contentType=HTML, el body ya contiene el HTML completo (solo se reemplazan variables).
     * Si contentType=TXT, retorna texto plano.
     */
    String render(NotificationTemplate template, Map<String, String> params);
}

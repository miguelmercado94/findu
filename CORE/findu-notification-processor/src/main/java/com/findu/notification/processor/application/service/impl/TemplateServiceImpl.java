package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.port.in.ResolveTemplateUseCase;
import com.findu.notification.processor.application.port.out.persistence.TemplateRepositoryPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.application.util.TemplateRenderer;
import com.findu.notification.processor.application.util.VariableValidator;
import com.findu.notification.processor.domain.enums.TemplateType;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class TemplateServiceImpl implements TemplateService, ResolveTemplateUseCase {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);
    private static final String DEFAULT_LANGUAGE = "es";

    private final TemplateRepositoryPort templateRepositoryPort;
    private final TemplateRenderer templateRenderer;
    private final VariableValidator variableValidator;

    public TemplateServiceImpl(TemplateRepositoryPort templateRepositoryPort,
                               TemplateRenderer templateRenderer,
                               VariableValidator variableValidator) {
        this.templateRepositoryPort = templateRepositoryPort;
        this.templateRenderer = templateRenderer;
        this.variableValidator = variableValidator;
    }

    @Override
    @Cacheable(value = "notification-templates", key = "#templateCode + '#' + #tipo + '#' + #language")
    public Optional<NotificationTemplate> findTemplate(String templateCode, TemplateType tipo, String language) {
        log.debug("Resolviendo template activo: code={} tipo={} language={}", templateCode, tipo, language);

        Optional<NotificationTemplate> template = templateRepositoryPort.findActive(templateCode, tipo, language);

        if (template.isEmpty() && !DEFAULT_LANGUAGE.equals(language)) {
            template = templateRepositoryPort.findActive(templateCode, tipo, DEFAULT_LANGUAGE);
        }

        return template.filter(NotificationTemplate::isActive);
    }

    @Override
    public Optional<NotificationTemplate> findTemplate(String templateCode, String channel, String language) {
        return findTemplate(templateCode, mapChannelToTipo(channel), language);
    }

    @Override
    public Optional<NotificationTemplate> resolveActive(String codigo, TemplateType tipo, String language) {
        return findTemplate(codigo, tipo, language);
    }

    @Override
    public String render(NotificationTemplate template, Map<String, String> params) {
        variableValidator.validate(template, params);
        return templateRenderer.render(template.getBodyTemplate(), params);
    }

    /**
     * Mapea el canal técnico a tipo funcional (PUSH → NOTIFICACION; resto 1:1 con el
     * nombre del {@link TemplateType}).
     */
    private TemplateType mapChannelToTipo(String channel) {
        String normalized = channel == null ? "" : channel.toUpperCase();
        if ("PUSH".equals(normalized)) {
            return TemplateType.NOTIFICACION;
        }
        return TemplateType.valueOf(normalized);
    }
}

package com.findu.notification.processor.application.service.impl;

import com.findu.notification.processor.application.port.output.TemplateRepositoryPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TemplateRepositoryPort templateRepositoryPort;

    public TemplateServiceImpl(TemplateRepositoryPort templateRepositoryPort) {
        this.templateRepositoryPort = templateRepositoryPort;
    }

    @Override
    @Cacheable(value = "notification-templates", key = "#templateCode + '#' + #channel + '#' + #language")
    public Optional<NotificationTemplate> findTemplate(String templateCode, String channel, String language) {
        log.debug("Buscando template: code={} channel={} language={}", templateCode, channel, language);

        Optional<NotificationTemplate> template = templateRepositoryPort
                .findByCodeAndChannelAndLanguage(templateCode, channel, language);

        if (template.isEmpty() && !"es".equals(language)) {
            template = templateRepositoryPort.findByCodeAndChannelAndLanguage(templateCode, channel, "es");
        }

        if (template.isEmpty()) {
            template = templateRepositoryPort.findByCodeAndChannel(templateCode, channel);
        }

        return template.filter(NotificationTemplate::isActive);
    }

    @Override
    public String render(NotificationTemplate template, Map<String, String> params) {
        String body = template.getBodyTemplate();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                body = body.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return body;
    }
}

package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FACTORY METHOD PATTERN — Crea la plantilla correcta según el canal.
 * Inyecta los senders y el TemplateService a cada implementación.
 */
@Component
public class NotificationTemplateFactory {

    private final Map<NotificationChannel, AbstractNotificationTemplate> templates;

    public NotificationTemplateFactory(List<NotificationSenderPort> senders, TemplateService templateService) {
        this.templates = new EnumMap<>(NotificationChannel.class);

        for (NotificationSenderPort sender : senders) {
            switch (sender.getChannel()) {
                case EMAIL -> templates.put(NotificationChannel.EMAIL, new EmailNotificationTemplate(sender, templateService));
                case SMS -> templates.put(NotificationChannel.SMS, new SmsNotificationTemplate(sender, templateService));
                case PUSH -> templates.put(NotificationChannel.PUSH, new PushNotificationTemplate(sender, templateService));
                case WHATSAPP -> templates.put(NotificationChannel.WHATSAPP, new WhatsappNotificationTemplate(sender, templateService));
            }
        }
    }

    public AbstractNotificationTemplate create(NotificationChannel channel) {
        AbstractNotificationTemplate template = templates.get(channel);
        if (template == null) {
            throw new IllegalArgumentException("No hay implementación para el canal: " + channel);
        }
        return template;
    }

    public AbstractNotificationTemplate create(String type) {
        return create(NotificationChannel.valueOf(type.toUpperCase()));
    }
}

package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import java.util.Map;

public class EmailNotificationTemplate extends AbstractNotificationTemplate {
    private final NotificationSenderPort sender;

    public EmailNotificationTemplate(NotificationSenderPort sender, TemplateService templateService) {
        super(templateService);
        this.sender = sender;
    }

    @Override public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }

    @Override protected String resolveRecipient(NotificationMessage message) { return message.getRecipient(); }

    @Override
    protected NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata) {
        log.info("[EMAIL] a={} subject={}", recipient, subject);
        return sender.send(recipient, subject, body, metadata);
    }
}

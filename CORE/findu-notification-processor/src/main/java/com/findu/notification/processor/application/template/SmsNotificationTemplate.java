package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import java.util.Map;

public class SmsNotificationTemplate extends AbstractNotificationTemplate {
    private final NotificationSenderPort sender;

    public SmsNotificationTemplate(NotificationSenderPort sender, TemplateService templateService) {
        super(templateService);
        this.sender = sender;
    }

    @Override public NotificationChannel getChannel() { return NotificationChannel.SMS; }

    @Override protected String resolveRecipient(NotificationMessage message) { return message.getRecipient(); }

    @Override
    protected String buildBody(NotificationMessage message) {
        String body = super.buildBody(message);
        return body.length() > 160 ? body.substring(0, 157) + "..." : body;
    }

    @Override
    protected NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata) {
        log.info("[SMS] a={} body={}", recipient, body);
        return sender.send(recipient, subject, body, metadata);
    }
}

package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import java.util.Map;

public class PushNotificationTemplate extends AbstractNotificationTemplate {
    private final NotificationSenderPort sender;

    public PushNotificationTemplate(NotificationSenderPort sender, TemplateService templateService) {
        super(templateService);
        this.sender = sender;
    }

    @Override public NotificationChannel getChannel() { return NotificationChannel.PUSH; }

    @Override protected String resolveRecipient(NotificationMessage message) { return message.getRecipient(); }

    @Override
    protected NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata) {
        // requiresConnection: si true, enviar via WebSocket/SSE (real-time); si false, via FCM directo al dispositivo
        boolean realTime = metadata != null && "true".equals(metadata.get("requiresConnection"));
        if (realTime) {
            log.info("[PUSH-REALTIME] Enviando via WebSocket/SSE a={} body={}", recipient, body);
        } else {
            log.info("[PUSH-FCM] Enviando via Firebase a={} body={}", recipient, body);
        }
        return sender.send(recipient, subject, body, metadata);
    }
}

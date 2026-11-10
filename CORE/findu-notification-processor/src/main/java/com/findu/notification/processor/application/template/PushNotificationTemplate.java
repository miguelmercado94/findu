package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.out.externalapi.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.enums.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import java.util.Map;

public class PushNotificationTemplate extends AbstractNotificationTemplate {
    /** Push generico de eventos (SERVICE_UPDATE, OFFER_RECEIVED, SYSTEM_ALERT): titulo/cuerpo ya vienen en params, sin DynamoDB. */
    public static final String EVENT_PUSH = "EVENT_PUSH";

    private final NotificationSenderPort sender;

    public PushNotificationTemplate(NotificationSenderPort sender, TemplateService templateService) {
        super(templateService);
        this.sender = sender;
    }

    @Override public NotificationChannel getChannel() { return NotificationChannel.PUSH; }

    @Override protected String resolveRecipient(NotificationMessage message) { return message.getRecipient(); }

    @Override
    protected String buildSubject(NotificationMessage message) {
        if (EVENT_PUSH.equalsIgnoreCase(message.getTemplateCode())) {
            Map<String, String> p = message.getParams();
            String title = p == null ? null : p.get("pushTitle");
            return title == null || title.isBlank() ? "FindU" : title;
        }
        if ("TICKET_MESSAGE_RECEIVED".equalsIgnoreCase(message.getTemplateCode()) && message.getParams() != null) {
            String ticketCode = message.getParams().getOrDefault("ticketCode", "");
            return "Soporte FindU - Ticket #" + ticketCode;
        }
        return super.buildSubject(message);
    }

    @Override
    protected String buildBody(NotificationMessage message) {
        if (EVENT_PUSH.equalsIgnoreCase(message.getTemplateCode())) {
            Map<String, String> p = message.getParams();
            String text = p == null ? null : p.get("pushBody");
            return text == null || text.isBlank() ? "Tienes una novedad en tu cuenta." : text;
        }
        if ("TICKET_MESSAGE_RECEIVED".equalsIgnoreCase(message.getTemplateCode()) && message.getParams() != null) {
            String senderName = message.getParams().getOrDefault("senderName", "Soporte FindU");
            String msg = message.getParams().getOrDefault("mensaje", "");
            return senderName + ": " + msg;
        }
        return super.buildBody(message);
    }

    @Override
    protected NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata) {
        // requiresConnection: si true, enviar via WebSocket/SSE (real-time); si false, via FCM directo al dispositivo
        boolean realTime = metadata != null && "true".equals(metadata.get("requiresConnection"));
        if (realTime) {
            log.info("[PUSH-REALTIME] Enviando via WebSocket/SSE a={} bodyLength={}", recipient, body == null ? 0 : body.length());
        } else {
            log.info("[PUSH-FCM] Enviando via Firebase a={} bodyLength={}", recipient, body == null ? 0 : body.length());
        }
        return sender.send(recipient, subject, body, metadata);
    }
}

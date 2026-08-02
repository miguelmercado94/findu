package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WhatsappSender implements NotificationSenderPort {
    private static final Logger log = LoggerFactory.getLogger(WhatsappSender.class);

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.WHATSAPP; }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        // TODO: Meta WhatsApp Business API — enviar mensaje con template aprobado
        log.info("[WHATSAPP] Mensaje enviado a={} body={}", recipient, body);
        return NotificationResult.builder().success(true).channel(NotificationChannel.WHATSAPP).recipient(recipient).build();
    }
}

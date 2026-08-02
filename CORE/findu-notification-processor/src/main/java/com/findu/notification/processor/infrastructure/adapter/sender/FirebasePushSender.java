package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class FirebasePushSender implements NotificationSenderPort {
    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.PUSH; }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        // TODO: Firebase Admin SDK — buscar device token por username y enviar
        log.info("[FCM] Push enviado a={} body={}", recipient, body);
        return NotificationResult.builder().success(true).channel(NotificationChannel.PUSH).recipient(recipient).build();
    }
}

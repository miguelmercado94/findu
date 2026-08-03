package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Envío de SMS via AWS SNS (LocalStack en dev).
 * El recipient debe incluir código de país (ej: +573003763300).
 */
@Component
public class SnsSmsender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SnsSmsender.class);

    private final SnsClient snsClient;

    public SnsSmsender(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SMS; }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(recipient)
                    .message(body)
                    .build();

            snsClient.publish(request);
            log.info("[SNS] SMS enviado exitosamente a={}", recipient);

            return NotificationResult.builder()
                    .success(true)
                    .channel(NotificationChannel.SMS)
                    .recipient(recipient)
                    .build();
        } catch (Exception e) {
            log.error("[SNS] Error enviando SMS a={}: {}", recipient, e.getMessage(), e);
            return NotificationResult.builder()
                    .success(false)
                    .channel(NotificationChannel.SMS)
                    .recipient(recipient)
                    .error(e.getMessage())
                    .build();
        }
    }
}

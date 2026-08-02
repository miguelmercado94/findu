package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SesEmailSender implements NotificationSenderPort {
    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        // TODO: AWS SES SDK real
        log.info("[SES] Email enviado a={} subject={}", recipient, subject);
        return NotificationResult.builder().success(true).channel(NotificationChannel.EMAIL).recipient(recipient).build();
    }
}

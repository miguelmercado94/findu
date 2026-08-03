package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

/**
 * Envío de emails via AWS SES (LocalStack en dev).
 * Soporta contenido HTML y TXT.
 * Los attachments se manejan como links en el body (SES basic no soporta adjuntos inline;
 * para adjuntos reales se necesita SES con RawMessage — se implementará cuando se active S3).
 */
@Component
public class SesEmailSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailSender(SesClient sesClient,
                          @Value("${findu.notification.aws.ses.from-email:no-reply@findu.co}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder().toAddresses(recipient).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("[SES] Email enviado exitosamente a={} subject={}", recipient, subject);

            return NotificationResult.builder()
                    .success(true)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipient)
                    .build();
        } catch (Exception e) {
            log.error("[SES] Error enviando email a={}: {}", recipient, e.getMessage(), e);
            return NotificationResult.builder()
                    .success(false)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipient)
                    .error(e.getMessage())
                    .build();
        }
    }
}

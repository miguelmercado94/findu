package com.findu.notification.processor.application.service;

import com.findu.notification.processor.application.template.AbstractNotificationTemplate;
import com.findu.notification.processor.application.template.NotificationTemplateFactory;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Orquestador principal. Usa el Factory para obtener el Template correcto
 * y ejecuta el Template Method para procesar la notificación.
 */
@Service
public class NotificationProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessorService.class);

    private final NotificationTemplateFactory factory;

    public NotificationProcessorService(NotificationTemplateFactory factory) {
        this.factory = factory;
    }

    public Mono<NotificationResult> process(NotificationMessage message) {
        log.info("Processing notification: id={} type={} recipient={}",
                message.getNotificationId(), message.getType(), message.getRecipient());

        try {
            AbstractNotificationTemplate template = factory.create(message.getType());
            return template.execute(message);
        } catch (Exception e) {
            log.error("Error creating template processor: {}", e.getMessage());
            return Mono.just(NotificationResult.builder()
                    .notificationId(message.getNotificationId())
                    .success(false)
                    .error(e.getMessage())
                    .build());
        }
    }
}

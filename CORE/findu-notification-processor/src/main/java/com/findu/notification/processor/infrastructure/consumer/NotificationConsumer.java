package com.findu.notification.processor.infrastructure.consumer;

import com.findu.notification.processor.application.service.NotificationProcessorService;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer unificado. Escucha todas las queues y delega al service
 * que usa Factory + Template Method para procesar.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationProcessorService processorService;

    public NotificationConsumer(NotificationProcessorService processorService) {
        this.processorService = processorService;
    }

    @RabbitListener(queues = "${findu.notification.queue.push}")
    public void handlePush(NotificationMessage message) {
        process(message, "PUSH");
    }

    @RabbitListener(queues = "${findu.notification.queue.email}")
    public void handleEmail(NotificationMessage message) {
        process(message, "EMAIL");
    }

    @RabbitListener(queues = "${findu.notification.queue.sms}")
    public void handleSms(NotificationMessage message) {
        process(message, "SMS");
    }

    @RabbitListener(queues = "${findu.notification.queue.whatsapp:findu.notifications.whatsapp}")
    public void handleWhatsapp(NotificationMessage message) {
        process(message, "WHATSAPP");
    }

    private void process(NotificationMessage message, String fallbackType) {
        if (message.getType() == null || message.getType().isBlank()) {
            message.setType(fallbackType);
        }
        NotificationResult result = processorService.process(message);
        if (!result.isSuccess()) {
            log.error("Notification failed: id={} channel={} error={}",
                    result.getNotificationId(), result.getChannel(), result.getError());
        }
    }
}

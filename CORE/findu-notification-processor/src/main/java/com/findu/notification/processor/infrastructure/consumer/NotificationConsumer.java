package com.findu.notification.processor.infrastructure.consumer;

import com.findu.notification.processor.application.service.NotificationProcessorService;
import com.findu.notification.processor.application.port.output.NotificationStatusRepositoryPort;
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
    private final NotificationStatusRepositoryPort statusRepository;

    public NotificationConsumer(NotificationProcessorService processorService,
                                NotificationStatusRepositoryPort statusRepository) {
        this.processorService = processorService;
        this.statusRepository = statusRepository;
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
        try {
            com.findu.notification.processor.domain.model.NotificationResult result = processorService.process(message).block();
            if (result == null || !result.isSuccess()) {
                String errorMsg = result != null ? result.getError() : "Unknown error";
                int retries = result != null ? result.getRetries() : 1;
                log.error("Notification failed after retries: id={} channel={} error={}",
                        message.getNotificationId(), fallbackType, errorMsg);
                statusRepository.saveFailure(message, "SEND_FAILURE", errorMsg, retries);
                throw new RuntimeException("Failed to process notification: " + errorMsg);
            }
            
            statusRepository.saveSuccess(message.getNotificationId());
        } catch (Exception e) {
            if (!(e instanceof RuntimeException && e.getMessage().startsWith("Failed to process notification"))) {
                log.error("Exception processing notification: id={} error={}", message.getNotificationId(), e.getMessage(), e);
                statusRepository.saveFailure(message, e.getClass().getSimpleName(), e.getMessage(), 3);
            }
            throw e;
        }
    }
}

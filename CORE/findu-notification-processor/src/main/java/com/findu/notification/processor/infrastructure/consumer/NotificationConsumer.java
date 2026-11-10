package com.findu.notification.processor.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.service.NotificationProcessorService;
import com.findu.notification.processor.application.port.out.persistence.NotificationStatusRepositoryPort;
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
    private final ObjectMapper objectMapper;
    private final FallbackPushMapper fallbackPushMapper;

    public NotificationConsumer(NotificationProcessorService processorService,
                                NotificationStatusRepositoryPort statusRepository,
                                ObjectMapper objectMapper,
                                FallbackPushMapper fallbackPushMapper) {
        this.processorService = processorService;
        this.statusRepository = statusRepository;
        this.objectMapper = objectMapper;
        this.fallbackPushMapper = fallbackPushMapper;
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

    /**
     * Listener de la cola Fallback (RabbitMQ).
     * Disparado por findu-notification-dispatcher cuando un usuario esta OFFLINE y recibe un evento.
     * {@link FallbackPushMapper} decide si el evento (chat de soporte, SERVICE_UPDATE de la lista blanca,
     * OFFER_RECEIVED, SYSTEM_ALERT con flag) se convierte en un PUSH (FCM). Un mensaje ilegible o no
     * soportado se descarta sin relanzar para que no se reencole en bucle.
     */
    @RabbitListener(queues = "${findu.rabbitmq.queues.fallback:findu.notifications.fallback.queue}")
    public void handleFallback(String rawJson) {
        log.info("Evento offline recibido desde cola fallback (length={})", rawJson == null ? 0 : rawJson.length());
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            fallbackPushMapper.map(root).ifPresent(pushMsg -> {
                log.info("Despachando Push FCM de fallback para usuario offline {}. templateCode={} type={}",
                        pushMsg.getRecipient(), pushMsg.getTemplateCode(),
                        pushMsg.getParams() == null ? null : pushMsg.getParams().get("type"));
                process(pushMsg, "PUSH");
            });
        } catch (Exception e) {
            log.error("Error al procesar mensaje fallback offline: {}", e.getMessage(), e);
        }
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

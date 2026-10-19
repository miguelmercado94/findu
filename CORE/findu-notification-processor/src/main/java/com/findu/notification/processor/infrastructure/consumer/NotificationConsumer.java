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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public NotificationConsumer(NotificationProcessorService processorService,
                                NotificationStatusRepositoryPort statusRepository,
                                ObjectMapper objectMapper) {
        this.processorService = processorService;
        this.statusRepository = statusRepository;
        this.objectMapper = objectMapper;
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
     * Disparado por findu-notification-dispatcher cuando un usuario está OFFLINE y recibe un evento.
     * Convierte el evento (p.ej. CHAT_MESSAGE de soporte) en una notificación PUSH (FCM) con el ticket y mensaje.
     */
    @RabbitListener(queues = "${findu.rabbitmq.queues.fallback:findu.notifications.fallback.queue}")
    public void handleFallback(String rawJson) {
        log.info("Evento offline recibido desde cola fallback: {}", rawJson);
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String eventType = root.path("eventType").asText("");
            String targetUserId = root.path("targetUserId").asText("");
            JsonNode payload = root.path("payload");

            if ("CHAT_MESSAGE".equalsIgnoreCase(eventType) || payload.has("content")) {
                String ticketCode = payload.path("ticketCode").asText("N/A");
                String content = payload.path("content").asText("");
                String senderName = payload.path("senderName").asText("Soporte FindU");

                Map<String, String> params = new HashMap<>();
                params.put("ticketCode", ticketCode);
                params.put("mensaje", content);
                params.put("senderName", senderName);

                NotificationMessage pushMsg = NotificationMessage.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .type("PUSH")
                        .recipient(targetUserId)
                        .templateCode("TICKET_MESSAGE_RECEIVED")
                        .language("es")
                        .params(params)
                        .requiresConnection(false)
                        .createdAt(Instant.now().toString())
                        .build();

                log.info("Despachando Push FCM de fallback para usuario offline {}. Ticket: {}, Mensaje: {}",
                        targetUserId, ticketCode, content);
                process(pushMsg, "PUSH");
            }
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

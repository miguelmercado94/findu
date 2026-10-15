package com.findu.notification.processor.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.dto.event.PendingValidationAlertEvent;
import com.findu.notification.processor.application.dto.event.ValidationResolvedEvent;
import com.findu.notification.processor.application.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertNotificationService alertNotificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${findu.notification.queue.alerts:findu.notifications.alerts}")
    public void consumeAlertMessage(Message message) {
        try {
            String routingKey = message.getMessageProperties() != null
                    ? message.getMessageProperties().getReceivedRoutingKey()
                    : null;
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Mensaje de alerta recibido en queue 'findu.notifications.alerts', routingKey='{}': {}", routingKey, body);

            JsonNode root = objectMapper.readTree(body);

            if ("alert.resolved".equals(routingKey) || root.has("resolution")) {
                ValidationResolvedEvent event = objectMapper.treeToValue(root, ValidationResolvedEvent.class);
                handleResolved(event);
            } else {
                PendingValidationAlertEvent event = objectMapper.treeToValue(root, PendingValidationAlertEvent.class);
                handlePending(event);
            }
        } catch (Exception e) {
            log.error("Error al procesar mensaje de alerta de validación: {}", e.getMessage(), e);
        }
    }

    public void handlePending(PendingValidationAlertEvent event) {
        log.info("Procesando PendingValidationAlertEvent: eventId={}, userId={}, validationCode={}",
                event.eventId(), event.userId(), event.validationCode());
        alertNotificationService.processPending(event);
    }

    public void handleResolved(ValidationResolvedEvent event) {
        log.info("Procesando ValidationResolvedEvent: eventId={}, userId={}, validationCode={}, resolution={}",
                event.eventId(), event.userId(), event.validationCode(), event.resolution());
        alertNotificationService.processResolved(event);
    }
}

package com.findu.notification.processor.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.notification.processor.application.port.out.AlertEventPublisherPort;
import com.findu.notification.processor.domain.model.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RabbitAlertEventPublisher implements AlertEventPublisherPort {

    public static final String ROUTING_KEY_ALERT = "findu.event.alert";

    private final RabbitTemplate rabbitTemplate;
    private final String eventsExchange;
    private final ObjectMapper objectMapper;

    public RabbitAlertEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${findu.rabbitmq.exchanges.events:findu.events.exchange}") String eventsExchange,
            ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventsExchange = eventsExchange;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(EventEnvelope<?> envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            log.info("Publicando EventEnvelope a exchange='{}', routingKey='{}', targetUserId='{}', eventType='{}'",
                    eventsExchange, ROUTING_KEY_ALERT, envelope.targetUserId(), envelope.eventType());
            Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(eventsExchange, ROUTING_KEY_ALERT, message);
        } catch (Exception e) {
            log.error("Error al publicar EventEnvelope: {}", e.getMessage(), e);
            throw new RuntimeException("Error publishing EventEnvelope", e);
        }
    }
}

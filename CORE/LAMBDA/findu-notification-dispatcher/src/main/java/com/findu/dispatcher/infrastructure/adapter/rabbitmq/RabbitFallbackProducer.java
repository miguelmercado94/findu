package com.findu.dispatcher.infrastructure.adapter.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.port.EventFallbackPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitFallbackProducer implements EventFallbackPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${findu.rabbitmq.exchanges.fallback:findu.notifications.fallback.exchange}")
    private String fallbackExchange;

    @Value("${findu.rabbitmq.routing-keys.fallback:findu.fallback.notification}")
    private String fallbackRoutingKey;

    @Override
    public Mono<Void> sendToFallbackQueue(EventEnvelope<?> event) {
        return Mono.fromRunnable(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                rabbitTemplate.convertAndSend(fallbackExchange, fallbackRoutingKey, eventJson);
                log.info("Usuario offline. Evento enrutado a cola de fallback RabbitMQ: targetUserId={}, eventId={}, eventType={}",
                        event.targetUserId(), event.eventId(), event.eventType());
            } catch (Exception e) {
                log.error("Error al publicar evento en cola de fallback RabbitMQ: {}", e.getMessage(), e);
                throw new RuntimeException("Fallo al despachar a cola fallback", e);
            }
        });
    }
}

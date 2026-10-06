package com.findu.dispatcher.infrastructure.adapter.rabbitmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.service.EventDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventConsumer {

    private final EventDispatcherService eventDispatcherService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${findu.rabbitmq.queues.dispatcher:findu.dispatcher.events.queue}")
    public void consumeEventMessage(String messageBody) {
        log.info("Mensaje consumido de RabbitMQ: {}", messageBody);
        try {
            EventEnvelope<Object> envelope = objectMapper.readValue(messageBody, new TypeReference<>() {});
            eventDispatcherService.dispatch(envelope).subscribe();
        } catch (Exception e) {
            log.error("Error al procesar mensaje consumido de RabbitMQ: {}", e.getMessage(), e);
        }
    }
}

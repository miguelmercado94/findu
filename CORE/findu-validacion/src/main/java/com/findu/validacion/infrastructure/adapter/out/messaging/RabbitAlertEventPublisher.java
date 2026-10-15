package com.findu.validacion.infrastructure.adapter.out.messaging;

import com.findu.validacion.application.dto.event.PendingValidationAlertEvent;
import com.findu.validacion.application.dto.event.ValidationResolvedEvent;
import com.findu.validacion.application.port.out.AlertEventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class RabbitAlertEventPublisher implements AlertEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public static final String ROUTING_KEY_PENDING = "alert.pending";
    public static final String ROUTING_KEY_RESOLVED = "alert.resolved";

    public RabbitAlertEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${findu.notification.exchange:findu.notifications}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public Mono<Void> publishPending(PendingValidationAlertEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Publishing PendingValidationAlertEvent to exchange='{}', routingKey='{}': eventId={}, userId={}, processCode={}, validationCode={}",
                    exchange, ROUTING_KEY_PENDING, event.eventId(), event.userId(), event.processCode(), event.validationCode());
            rabbitTemplate.convertAndSend(exchange, ROUTING_KEY_PENDING, event);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> publishResolved(ValidationResolvedEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Publishing ValidationResolvedEvent to exchange='{}', routingKey='{}': eventId={}, userId={}, validationCode={}, resolution={}",
                    exchange, ROUTING_KEY_RESOLVED, event.eventId(), event.userId(), event.validationCode(), event.resolution());
            rabbitTemplate.convertAndSend(exchange, ROUTING_KEY_RESOLVED, event);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}

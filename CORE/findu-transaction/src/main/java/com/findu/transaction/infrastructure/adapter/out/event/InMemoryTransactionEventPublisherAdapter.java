package com.findu.transaction.infrastructure.adapter.out.event;

import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.application.port.out.event.TransactionEventPublisher;
import com.findu.transaction.domain.event.DomainEvent;
import com.findu.transaction.domain.event.FinancialEventEnvelope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Primary
@Component
public class InMemoryTransactionEventPublisherAdapter implements TransactionEventPublisher, EventPublisherPort {

    @Getter
    private final List<FinancialEventEnvelope> publishedEvents = Collections.synchronizedList(new ArrayList<>());
    private final RestClient restClient;

    public InMemoryTransactionEventPublisherAdapter(
            @Value("${findu.notification.processor.url:http://localhost:8088}") String notificationProcessorUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(notificationProcessorUrl)
                .build();
    }

    @Override
    public void publish(FinancialEventEnvelope eventEnvelope) {
        if (eventEnvelope == null) return;

        publishedEvents.add(eventEnvelope);
        log.info("FinancialEventEnvelope publicado [eventId={}, eventType={}, aggregateType={}, aggregateId={}]",
                eventEnvelope.getEventId(),
                eventEnvelope.getEventType(),
                eventEnvelope.getAggregateType(),
                eventEnvelope.getAggregateId());

        try {
            restClient.post()
                    .uri("/api/v1/notifications/events/financial")
                    .body(eventEnvelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento financiero transmitido exitosamente a findu-notification-processor: eventId={}", eventEnvelope.getEventId());
        } catch (Exception e) {
            log.warn("Notificación asíncrona enviada a logger local (notification-processor no alcanzable o modo in-memory): eventId={}, error={}",
                    eventEnvelope.getEventId(), e.getMessage());
        }
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;
        log.info("DomainEvent genérico recibido: {}", event.getClass().getSimpleName());
        if (event instanceof FinancialEventEnvelope envelope) {
            publish(envelope);
        }
    }

    public void clearPublishedEvents() {
        publishedEvents.clear();
    }
}

package com.findu.help.v2.infrastructure.adapter.out.event;

import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.domain.event.*;
import com.findu.help.v2.infrastructure.adapter.out.event.dto.EventEnvelopeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class HttpRealtimeEventPublisher implements RealtimeEventPublisher {

    private final RestClient restClient;
    private final String dispatcherUrl;

    public HttpRealtimeEventPublisher(
            @Value("${findu.notification.dispatcher.url:http://localhost:9000}") String dispatcherUrl) {
        this.dispatcherUrl = dispatcherUrl;
        this.restClient = RestClient.builder()
                .baseUrl(dispatcherUrl)
                .build();
    }

    @Override
    public void publishRealtime(DomainEvent event) {
        if (event == null) {
            return;
        }

        String eventType = "SERVICE_UPDATE";
        String targetUserId = "";

        if (event instanceof TicketMessageAddedEvent e) {
            eventType = "CHAT_MESSAGE";
            targetUserId = "USER_" + e.getSenderId();
        } else if (event instanceof TicketCreatedEvent e) {
            eventType = "SERVICE_UPDATE";
            targetUserId = "CLIENTE_" + e.getUserId();
        } else if (event instanceof TicketAssignedEvent e) {
            eventType = "SERVICE_UPDATE";
            targetUserId = "AGENTE_" + e.getAgentId();
        } else if (event instanceof TicketClosedEvent e) {
            eventType = "SERVICE_UPDATE";
            targetUserId = "CLIENTE_" + e.getUserId();
        } else if (event instanceof TicketResolvedEvent e) {
            eventType = "SERVICE_UPDATE";
            targetUserId = "CLIENTE_TICKET";
        }

        EventEnvelopeDto<DomainEvent> envelope = EventEnvelopeDto.<DomainEvent>builder()
                .eventType(eventType)
                .eventId(UUID.randomUUID().toString())
                .timestamp(event.occurredOn())
                .targetUserId(targetUserId)
                .payload(event)
                .build();

        try {
            restClient.post()
                    .uri("/api/v1/canales/dispatchNotification")
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Evento en tiempo real enviado vía HTTP al dispatcher [Target: {}, EventType: {}]",
                    targetUserId, eventType);
        } catch (Exception e) {
            log.error("Error al enviar evento HTTP al dispatcher {}: {}", dispatcherUrl, e.getMessage(), e);
        }
    }
}

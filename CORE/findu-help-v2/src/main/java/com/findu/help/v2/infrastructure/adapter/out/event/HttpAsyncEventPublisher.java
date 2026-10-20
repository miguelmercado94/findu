package com.findu.help.v2.infrastructure.adapter.out.event;

import com.findu.help.v2.application.port.out.event.AsyncEventPublisher;
import com.findu.help.v2.domain.event.*;
import com.findu.help.v2.infrastructure.adapter.out.event.dto.DispatchNotificationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class HttpAsyncEventPublisher implements AsyncEventPublisher {

    private final RestClient restClient;
    private final String dispatcherUrl;

    public HttpAsyncEventPublisher(
            @Value("${findu.notification.dispatcher.url:http://localhost:9000}") String dispatcherUrl) {
        this.dispatcherUrl = dispatcherUrl;
        this.restClient = RestClient.builder()
                .baseUrl(dispatcherUrl)
                .build();
    }

    @Override
    public void publishAsync(DomainEvent event) {
        if (event == null) {
            return;
        }

        String recipient = "SYSTEM";
        String templateCode = "TICKET_EVENT";
        String type = "PUSH";
        Map<String, Object> params = new HashMap<>();

        if (event instanceof TicketMessageAddedEvent e) {
            recipient = "USER_" + e.getSenderId();
            templateCode = "TICKET_MESSAGE_NEW";
            params.put("ticketId", e.getTicketId());
            params.put("ticketCode", e.getTicketCode());
            params.put("senderName", e.getSenderName());
            params.put("content", e.getContent());
        } else if (event instanceof TicketCreatedEvent e) {
            recipient = "CLIENTE_" + e.getUserId();
            templateCode = "TICKET_CREATED";
            params.put("ticketId", e.getTicketId());
            params.put("ticketCode", e.getTicketCode());
            params.put("solicitudId", e.getSolicitudId());
        } else if (event instanceof TicketAssignedEvent e) {
            recipient = "AGENTE_" + e.getAgentId();
            templateCode = "TICKET_ASSIGNED";
            params.put("ticketId", e.getTicketId());
            params.put("ticketCode", e.getTicketCode());
            params.put("agentName", e.getAgentName());
        } else if (event instanceof TicketResolvedEvent e) {
            recipient = (e.getRequesterEmail() != null && !e.getRequesterEmail().isBlank())
                    ? e.getRequesterEmail()
                    : "usuario" + e.getRequesterUserId() + "@findu.com";
            type = "EMAIL";
            templateCode = "TICKET_RESOLVED";
            params.put("ticketId", e.getTicketId());
            params.put("ticketCode", e.getTicketCode());
            params.put("actionType", e.getActionType() != null ? e.getActionType().name() : "");
            params.put("subject", e.getSubject() != null ? e.getSubject() : "");
            params.put("resolutionDetail", e.getResolutionDetail() != null ? e.getResolutionDetail() : "");
            params.put("agentName", e.getAssignedAgentName() != null ? e.getAssignedAgentName() : "");
            params.put("status", "RESUELTO");
        } else if (event instanceof TicketClosedEvent e) {
            recipient = "CLIENTE_" + e.getUserId();
            templateCode = "TICKET_CLOSED";
            params.put("ticketId", e.getTicketId());
            params.put("ticketCode", e.getTicketCode());
        }

        DispatchNotificationRequestDto requestDto = DispatchNotificationRequestDto.builder()
                .type(type)
                .recipient(recipient)
                .templateCode(templateCode)
                .language("es")
                .params(params)
                .build();

        try {
            restClient.post()
                    .uri("/api/v1/canales/dispatchNotification")
                    .body(requestDto)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notificación asíncrona enviada a findu-notification-dispatcher [/api/v1/canales/dispatchNotification] (Recipient: {}, Template: {})",
                    recipient, templateCode);
        } catch (Exception e) {
            log.error("Error al enviar notificación asíncrona a findu-notification-dispatcher (URL: {}): {}",
                    dispatcherUrl, e.getMessage(), e);
        }
    }
}

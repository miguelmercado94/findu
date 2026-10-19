package com.findu.help.v2.infrastructure.adapter.out.event;

import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.event.*;
import com.findu.help.v2.infrastructure.adapter.out.event.dto.DispatchNotificationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

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

        if (event instanceof TicketMessageAddedEvent e) {
            String eventType = "CHAT_MESSAGE";
            if (e.getSenderType() == SenderType.USUARIO) {
                // Mensaje enviado por usuario/proveedor hacia soporte
                if (e.getAssignedAgentId() != null) {
                    dispatch("AGENTE_" + e.getAssignedAgentId(), eventType, e);
                }
                dispatch("SOPORTE_GENERAL", eventType, e);
            } else {
                // Mensaje de respuesta del soporte hacia el proveedor/usuario
                String role = e.getRequesterRole() != null ? e.getRequesterRole().name() : "PROVEEDOR";
                Long reqId = e.getRequesterUserId() != null ? e.getRequesterUserId() : e.getSenderId();
                dispatch(role + "_" + reqId, eventType, e);
            }
        } else if (event instanceof TicketCreatedEvent e) {
            String eventType = "TICKET_CREATED";
            // Notificar al escritorio de soporte general en tiempo real
            dispatch("SOPORTE_GENERAL", eventType, e);

            // Confirmación en canal del solicitante
            String role = e.getUserRole() != null ? e.getUserRole().name() : "PROVEEDOR";
            dispatch(role + "_" + e.getUserId(), "SERVICE_UPDATE", e);
        } else if (event instanceof TicketAssignedEvent e) {
            String eventType = "TICKET_ASSIGNED";
            // Activar WebSocket con la app del solicitante (proveedor/cliente)
            String role = e.getRequesterRole() != null ? e.getRequesterRole().name() : "PROVEEDOR";
            if (e.getRequesterUserId() != null) {
                dispatch(role + "_" + e.getRequesterUserId(), eventType, e);
            }
            if (e.getAgentId() != null) {
                dispatch("AGENTE_" + e.getAgentId(), eventType, e);
            }
            dispatch("SOPORTE_GENERAL", eventType, e);
        } else if (event instanceof TicketClosedEvent e) {
            String eventType = "TICKET_CLOSED";
            dispatch("CLIENTE_" + e.getUserId(), eventType, e);
            dispatch("PROVEEDOR_" + e.getUserId(), eventType, e);
            dispatch("SOPORTE_GENERAL", eventType, e);
        } else if (event instanceof TicketResolvedEvent e) {
            String eventType = "TICKET_RESOLVED";
            dispatch("SOPORTE_GENERAL", eventType, e);
        }
    }

    private void dispatch(String targetRecipient, String eventType, DomainEvent event) {
        Map<String, Object> params = new LinkedHashMap<>();

        if (event instanceof TicketMessageAddedEvent m) {
            params.put("ticketId", m.getTicketId());
            params.put("ticketCode", m.getTicketCode());
            params.put("messageId", m.getMessageId());
            params.put("senderId", m.getSenderId());
            params.put("senderType", m.getSenderType() != null ? m.getSenderType().name() : "USUARIO");
            params.put("senderName", m.getSenderName());
            params.put("content", m.getContent());
            params.put("isInternalNote", m.getIsInternalNote());
        } else if (event instanceof TicketCreatedEvent c) {
            params.put("ticketId", c.getTicketId());
            params.put("ticketCode", c.getTicketCode());
            params.put("userId", c.getUserId());
            params.put("userRole", c.getUserRole() != null ? c.getUserRole().name() : "");
            params.put("categoryName", c.getCategoryName());
            params.put("subcategoryName", c.getSubcategoryName());
            params.put("subject", c.getSubject());
            params.put("description", c.getDescription());
            params.put("priority", c.getPriority() != null ? c.getPriority().name() : "MEDIA");
        } else if (event instanceof TicketAssignedEvent a) {
            params.put("ticketId", a.getTicketId());
            params.put("ticketCode", a.getTicketCode());
            params.put("agentId", a.getAgentId());
            params.put("agentName", a.getAgentName());
            params.put("agentEmail", a.getAgentEmail());
            params.put("status", "ASIGNADO");
        } else if (event instanceof TicketClosedEvent cl) {
            params.put("ticketId", cl.getTicketId());
            params.put("ticketCode", cl.getTicketCode());
            params.put("userId", cl.getUserId());
            params.put("status", "CERRADO");
        } else if (event instanceof TicketResolvedEvent r) {
            params.put("ticketId", r.getTicketId());
            params.put("ticketCode", r.getTicketCode());
            params.put("actionType", r.getActionType() != null ? r.getActionType().name() : "");
            params.put("status", "RESUELTO");
        }

        params.put("eventType", eventType);
        params.put("occurredOn", event.occurredOn().toString());

        DispatchNotificationRequestDto requestDto = DispatchNotificationRequestDto.builder()
                .type(eventType)
                .recipient(targetRecipient)
                .templateCode(eventType)
                .language("es")
                .params(params)
                .build();

        try {
            restClient.post()
                    .uri("/api/v1/canales/dispatchNotification")
                    .body(requestDto)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Evento en tiempo real enviado al dispatcher [/api/v1/canales/dispatchNotification] (Recipient: {}, EventType: {})",
                    targetRecipient, eventType);
        } catch (Exception e) {
            log.error("Error al enviar evento en tiempo real a dispatcher {}: {}", dispatcherUrl, e.getMessage());
        }
    }
}

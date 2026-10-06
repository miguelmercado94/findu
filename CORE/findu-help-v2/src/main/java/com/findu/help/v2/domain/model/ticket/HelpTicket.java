package com.findu.help.v2.domain.model.ticket;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.ResolutionActionType;
import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.event.*;
import com.findu.help.v2.domain.model.priority.FixedPriorityCalculator;
import com.findu.help.v2.domain.model.priority.PriorityCalculator;
import com.findu.help.v2.domain.valueobject.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class HelpTicket {

    private String id;
    private TicketCode ticketCode;
    private TicketRequester requester;

    private Long solicitudId;
    private String subcategoryId;
    private String categoryName;
    private String subcategoryName;

    private TicketStatus status;
    private PriorityLevel priority;
    private Integer priorityScore;

    private AssignedAgent assignedAgent;

    private String subject;
    private String description;
    private Location location;

    private final List<TicketMessage> messages = new ArrayList<>();
    private TicketResolution resolution;
    private final List<TicketStatusHistory> statusHistory = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    // Private Constructor for Aggregate Root
    private HelpTicket() {}

    /**
     * Factory Method de creación del Agregado
     */
    public static HelpTicket create(
            TicketRequester requester,
            Long solicitudId,
            String subcategoryId,
            String categoryName,
            String subcategoryName,
            PriorityLevel priority,
            String subject,
            String description,
            Location location,
            List<MessageAttachment> initialAttachments,
            PriorityCalculator priorityCalculator) {

        if (requester == null) {
            throw new IllegalArgumentException("El solicitante es obligatorio");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        HelpTicket ticket = new HelpTicket();
        ticket.id = UUID.randomUUID().toString();
        ticket.ticketCode = TicketCode.generate("HT");
        ticket.requester = requester;
        ticket.solicitudId = solicitudId;
        ticket.subcategoryId = subcategoryId;
        ticket.categoryName = categoryName != null ? categoryName : "";
        ticket.subcategoryName = subcategoryName != null ? subcategoryName : "";

        ticket.priority = priority != null ? priority : PriorityLevel.MEDIA;
        PriorityCalculator calculator = priorityCalculator != null ? priorityCalculator : new FixedPriorityCalculator();
        ticket.priorityScore = calculator.calculateScore(ticket.priority);

        ticket.status = TicketStatus.ABIERTO;
        ticket.subject = subject.trim();
        ticket.description = description.trim();
        ticket.location = location;

        Instant now = Instant.now();
        ticket.createdAt = now;
        ticket.updatedAt = now;

        // Historial inicial
        ticket.statusHistory.add(new TicketStatusHistory(TicketStatus.ABIERTO, requester.getUserId(), "Ticket creado por usuario", now));

        // Primer mensaje
        TicketMessage initialMessage = new TicketMessage(
                "MSG-" + UUID.randomUUID().toString().substring(0, 8),
                requester.getUserId(),
                SenderType.USUARIO,
                requester.getName(),
                description,
                false,
                initialAttachments,
                now
        );
        ticket.messages.add(initialMessage);

        // Registro de Evento de Dominio
        ticket.uncommittedEvents.add(new TicketCreatedEvent(
                ticket.id,
                ticket.ticketCode.getValue(),
                requester.getUserId(),
                requester.getUserRole(),
                solicitudId,
                ticket.priority
        ));

        return ticket;
    }

    /**
     * Reconstrucción desde Persistencia (infraestructura)
     */
    public static HelpTicket reconstitute(
            String id,
            TicketCode ticketCode,
            TicketRequester requester,
            Long solicitudId,
            String subcategoryId,
            String categoryName,
            String subcategoryName,
            TicketStatus status,
            PriorityLevel priority,
            Integer priorityScore,
            AssignedAgent assignedAgent,
            String subject,
            String description,
            Location location,
            List<TicketMessage> messages,
            TicketResolution resolution,
            List<TicketStatusHistory> statusHistory,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt) {

        HelpTicket ticket = new HelpTicket();
        ticket.id = id;
        ticket.ticketCode = ticketCode;
        ticket.requester = requester;
        ticket.solicitudId = solicitudId;
        ticket.subcategoryId = subcategoryId;
        ticket.categoryName = categoryName;
        ticket.subcategoryName = subcategoryName;
        ticket.status = status;
        ticket.priority = priority;
        ticket.priorityScore = priorityScore;
        ticket.assignedAgent = assignedAgent;
        ticket.subject = subject;
        ticket.description = description;
        ticket.location = location;

        if (messages != null) ticket.messages.addAll(messages);
        ticket.resolution = resolution;
        if (statusHistory != null) ticket.statusHistory.addAll(statusHistory);

        ticket.createdAt = createdAt;
        ticket.updatedAt = updatedAt;
        ticket.closedAt = closedAt;

        return ticket;
    }

    /**
     * Regla de negocio: Asignar / Tomar Ticket (ABIERTO -> ASIGNADO)
     */
    public void assignTo(AssignedAgent agent) {
        if (this.status != TicketStatus.ABIERTO) {
            throw new IllegalStateException("Solo se pueden tomar o asignar tickets en estado ABIERTO. Estado actual: " + this.status);
        }
        if (agent == null) {
            throw new IllegalArgumentException("El agente a asignar es obligatorio");
        }

        this.assignedAgent = agent;
        this.status = TicketStatus.ASIGNADO;
        Instant now = Instant.now();
        this.updatedAt = now;

        this.statusHistory.add(new TicketStatusHistory(TicketStatus.ASIGNADO, agent.getAgentId(), "Ticket tomado por agente: " + agent.getName(), now));

        this.uncommittedEvents.add(new TicketAssignedEvent(
                this.id,
                this.ticketCode.getValue(),
                agent.getAgentId(),
                agent.getName(),
                agent.getEmail()
        ));
    }

    /**
     * Regla de negocio: Enviar mensaje al chat
     */
    public TicketMessage addMessage(Long senderId, SenderType senderType, String senderName, String content, Boolean isInternalNote, List<MessageAttachment> attachments) {
        if (this.status == TicketStatus.CERRADO) {
            throw new IllegalStateException("No se pueden agregar mensajes a un ticket CERRADO.");
        }

        Instant now = Instant.now();
        String msgId = "MSG-" + UUID.randomUUID().toString().substring(0, 8);

        TicketMessage message = new TicketMessage(msgId, senderId, senderType, senderName, content, isInternalNote, attachments, now);
        this.messages.add(message);
        this.updatedAt = now;

        this.uncommittedEvents.add(new TicketMessageAddedEvent(
                this.id,
                this.ticketCode.getValue(),
                msgId,
                senderId,
                senderType,
                senderName,
                content,
                isInternalNote
        ));

        return message;
    }

    /**
     * Regla de negocio: Resolver Ticket (ASIGNADO -> RESUELTO)
     */
    public void resolve(ResolutionActionType actionType, BigDecimal compensationAmount, String agentNotes) {
        if (this.status != TicketStatus.ASIGNADO) {
            throw new IllegalStateException("Solo se puede resolver un ticket en estado ASIGNADO. Estado actual: " + this.status);
        }

        Instant now = Instant.now();
        this.resolution = new TicketResolution(actionType, compensationAmount, agentNotes, now);
        this.status = TicketStatus.RESUELTO;
        this.updatedAt = now;

        Long agentId = this.assignedAgent != null ? this.assignedAgent.getAgentId() : 0L;
        this.statusHistory.add(new TicketStatusHistory(TicketStatus.RESUELTO, agentId, "Ticket resuelto con acción: " + actionType, now));

        this.uncommittedEvents.add(new TicketResolvedEvent(
                this.id,
                this.ticketCode.getValue(),
                actionType,
                compensationAmount
        ));
    }

    /**
     * Regla de negocio: Cerrar Ticket (RESUELTO -> CERRADO)
     */
    public void close(Long closedByUserId) {
        if (this.status != TicketStatus.RESUELTO) {
            throw new IllegalStateException("Solo se puede cerrar un ticket en estado RESUELTO. Estado actual: " + this.status);
        }

        Instant now = Instant.now();
        this.status = TicketStatus.CERRADO;
        this.closedAt = now;
        this.updatedAt = now;

        this.statusHistory.add(new TicketStatusHistory(TicketStatus.CERRADO, closedByUserId, "Ticket cerrado definitivamente", now));

        this.uncommittedEvents.add(new TicketClosedEvent(
                this.id,
                this.ticketCode.getValue(),
                this.requester.getUserId(),
                now
        ));
    }

    public List<TicketMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<TicketStatusHistory> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public List<DomainEvent> pullUncommittedEvents() {
        List<DomainEvent> events = new ArrayList<>(this.uncommittedEvents);
        this.uncommittedEvents.clear();
        return events;
    }
}

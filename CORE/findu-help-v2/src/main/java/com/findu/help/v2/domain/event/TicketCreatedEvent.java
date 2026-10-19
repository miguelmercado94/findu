package com.findu.help.v2.domain.event;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TicketCreatedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final Long userId;
    private final UserRole userRole;
    private final Long solicitudId;
    private final PriorityLevel priority;
    private final String categoryName;
    private final String subcategoryName;
    private final String subject;
    private final String description;
    private final Instant occurredOn;

    public TicketCreatedEvent(String ticketId, String ticketCode, Long userId, UserRole userRole,
                              Long solicitudId, PriorityLevel priority,
                              String categoryName, String subcategoryName, String subject, String description) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.userId = userId;
        this.userRole = userRole;
        this.solicitudId = solicitudId;
        this.priority = priority;
        this.categoryName = categoryName;
        this.subcategoryName = subcategoryName;
        this.subject = subject;
        this.description = description;
        this.occurredOn = Instant.now();
    }

    public TicketCreatedEvent(String ticketId, String ticketCode, Long userId, UserRole userRole, Long solicitudId, PriorityLevel priority) {
        this(ticketId, ticketCode, userId, userRole, solicitudId, priority, "", "", "", "");
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

package com.findu.help.v2.domain.event;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TicketAssignedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final Long agentId;
    private final String agentName;
    private final String agentEmail;
    private final Long requesterUserId;
    private final UserRole requesterRole;
    private final Instant occurredOn;

    public TicketAssignedEvent(String ticketId, String ticketCode, Long agentId, String agentName, String agentEmail,
                              Long requesterUserId, UserRole requesterRole) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentEmail = agentEmail;
        this.requesterUserId = requesterUserId;
        this.requesterRole = requesterRole;
        this.occurredOn = Instant.now();
    }

    public TicketAssignedEvent(String ticketId, String ticketCode, Long agentId, String agentName, String agentEmail) {
        this(ticketId, ticketCode, agentId, agentName, agentEmail, null, null);
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

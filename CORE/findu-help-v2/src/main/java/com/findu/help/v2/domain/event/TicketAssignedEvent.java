package com.findu.help.v2.domain.event;

import lombok.Getter;

import java.time.Instant;

@Getter
public class TicketAssignedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final Long agentId;
    private final String agentName;
    private final String agentEmail;
    private final Instant occurredOn;

    public TicketAssignedEvent(String ticketId, String ticketCode, Long agentId, String agentName, String agentEmail) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentEmail = agentEmail;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

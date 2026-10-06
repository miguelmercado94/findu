package com.findu.help.v2.domain.event;

import lombok.Getter;

import java.time.Instant;

@Getter
public class TicketClosedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final Long userId;
    private final Instant closedAt;
    private final Instant occurredOn;

    public TicketClosedEvent(String ticketId, String ticketCode, Long userId, Instant closedAt) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.userId = userId;
        this.closedAt = closedAt;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

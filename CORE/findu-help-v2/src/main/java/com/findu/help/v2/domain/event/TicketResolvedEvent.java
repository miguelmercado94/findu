package com.findu.help.v2.domain.event;

import com.findu.help.v2.domain.enums.ResolutionActionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class TicketResolvedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final ResolutionActionType actionType;
    private final BigDecimal compensationAmount;
    private final Instant occurredOn;

    public TicketResolvedEvent(String ticketId, String ticketCode, ResolutionActionType actionType, BigDecimal compensationAmount) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.actionType = actionType;
        this.compensationAmount = compensationAmount;
        this.occurredOn = Instant.now();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

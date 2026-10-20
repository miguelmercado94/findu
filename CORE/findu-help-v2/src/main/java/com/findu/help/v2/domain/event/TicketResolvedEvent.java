package com.findu.help.v2.domain.event;

import com.findu.help.v2.domain.enums.ResolutionActionType;
import com.findu.help.v2.domain.enums.UserRole;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class TicketResolvedEvent implements DomainEvent {
    private final String ticketId;
    private final String ticketCode;
    private final ResolutionActionType actionType;
    private final BigDecimal compensationAmount;
    private final Long requesterUserId;
    private final UserRole requesterRole;
    private final String requesterName;
    private final String requesterEmail;
    private final String subject;
    private final String resolutionDetail;
    private final String assignedAgentName;
    private final Instant occurredOn;

    public TicketResolvedEvent(
            String ticketId,
            String ticketCode,
            ResolutionActionType actionType,
            BigDecimal compensationAmount,
            Long requesterUserId,
            UserRole requesterRole,
            String requesterName,
            String requesterEmail,
            String subject,
            String resolutionDetail,
            String assignedAgentName) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.actionType = actionType;
        this.compensationAmount = compensationAmount;
        this.requesterUserId = requesterUserId;
        this.requesterRole = requesterRole;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.subject = subject;
        this.resolutionDetail = resolutionDetail;
        this.assignedAgentName = assignedAgentName;
        this.occurredOn = Instant.now();
    }

    public TicketResolvedEvent(String ticketId, String ticketCode, ResolutionActionType actionType, BigDecimal compensationAmount) {
        this(ticketId, ticketCode, actionType, compensationAmount, null, null, null, null, null, null, null);
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}

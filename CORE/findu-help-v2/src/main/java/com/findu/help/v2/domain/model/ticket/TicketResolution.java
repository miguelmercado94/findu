package com.findu.help.v2.domain.model.ticket;

import com.findu.help.v2.domain.enums.ResolutionActionType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class TicketResolution {
    private final ResolutionActionType actionType;
    private final BigDecimal compensationAmount;
    private final String agentNotes;
    private final Instant resolvedAt;

    public TicketResolution(ResolutionActionType actionType, BigDecimal compensationAmount, String agentNotes, Instant resolvedAt) {
        if (actionType == null) {
            throw new IllegalArgumentException("El tipo de acción de resolución no puede ser nulo");
        }
        this.actionType = actionType;
        this.compensationAmount = compensationAmount != null ? compensationAmount : BigDecimal.ZERO;
        this.agentNotes = agentNotes != null ? agentNotes.trim() : "";
        this.resolvedAt = resolvedAt != null ? resolvedAt : Instant.now();
    }
}

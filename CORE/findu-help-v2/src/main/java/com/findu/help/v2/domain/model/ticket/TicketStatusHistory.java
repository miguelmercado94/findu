package com.findu.help.v2.domain.model.ticket;

import com.findu.help.v2.domain.enums.TicketStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class TicketStatusHistory {
    private final TicketStatus status;
    private final Long changedBy;
    private final String reason;
    private final Instant changedAt;

    public TicketStatusHistory(TicketStatus status, Long changedBy, String reason, Instant changedAt) {
        if (status == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        this.status = status;
        this.changedBy = changedBy;
        this.reason = reason != null ? reason.trim() : "";
        this.changedAt = changedAt != null ? changedAt : Instant.now();
    }
}

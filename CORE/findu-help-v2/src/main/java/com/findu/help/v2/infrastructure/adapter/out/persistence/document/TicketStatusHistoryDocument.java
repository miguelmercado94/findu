package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import com.findu.help.v2.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusHistoryDocument {
    private TicketStatus status;
    private Long changedBy;
    private String reason;
    private Instant changedAt;
}

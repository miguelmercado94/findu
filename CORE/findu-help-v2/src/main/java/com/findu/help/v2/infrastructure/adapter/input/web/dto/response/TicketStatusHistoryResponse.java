package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

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
public class TicketStatusHistoryResponse {
    private TicketStatus status;
    private Long changedBy;
    private String reason;
    private Instant changedAt;
}

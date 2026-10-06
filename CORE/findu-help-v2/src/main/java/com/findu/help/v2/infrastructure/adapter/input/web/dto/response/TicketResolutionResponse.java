package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import com.findu.help.v2.domain.enums.ResolutionActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResolutionResponse {
    private ResolutionActionType actionType;
    private BigDecimal compensationAmount;
    private String agentNotes;
    private Instant resolvedAt;
}

package com.findu.help.v2.infrastructure.adapter.input.web.dto.request;

import com.findu.help.v2.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTicketStatusRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private TicketStatus newStatus;

    private Long agentId;
    private String agentName;
    private String agentEmail;
    private String reason;
}

package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpTicketResponse {
    private String id;
    private String ticketCode;
    private TicketRequesterResponse requester;

    private Long solicitudId;
    private String subcategoryId;
    private String categoryName;
    private String subcategoryName;

    private TicketStatus status;
    private PriorityLevel priority;
    private Integer priorityScore;

    private AssignedAgentResponse assignedAgent;

    private String subject;
    private String description;
    private LocationResponse location;

    private List<TicketMessageResponse> messages;
    private TicketResolutionResponse resolution;
    private List<TicketStatusHistoryResponse> statusHistory;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
}

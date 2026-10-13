package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "help_tickets")
public class HelpTicketDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ticketCode;

    @Indexed(unique = true, sparse = true)
    private String externalRef;

    private TicketRequesterDocument requester;
    private Long solicitudId;
    private String subcategoryId;
    private String categoryName;
    private String subcategoryName;

    private TicketStatus status;
    private PriorityLevel priority;
    private Integer priorityScore;

    private AssignedAgentDocument assignedAgent;
    private String subject;
    private String description;
    private LocationDocument location;

    private List<TicketMessageDocument> messages;
    private TicketResolutionDocument resolution;
    private List<TicketStatusHistoryDocument> statusHistory;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
}

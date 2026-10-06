package com.findu.help.v2.application.port.in.command;

import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.valueobject.AssignedAgent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangeTicketStatusCommand {
    private final String ticketReference;
    private final TicketStatus newStatus;
    private final AssignedAgent assignedAgent;
    private final String reason;
}

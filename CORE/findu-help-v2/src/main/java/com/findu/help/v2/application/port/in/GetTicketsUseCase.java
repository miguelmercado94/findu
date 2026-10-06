package com.findu.help.v2.application.port.in;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public interface GetTicketsUseCase {

    List<HelpTicket> execute(FilterQuery query);

    @Getter
    @Builder
    class FilterQuery {
        private TicketStatus status;
        private PriorityLevel priority;
        private UserRole userRole;
        private Long userId;
        private Long assignedAgentId;
        private Integer page;
        private Integer size;
    }
}

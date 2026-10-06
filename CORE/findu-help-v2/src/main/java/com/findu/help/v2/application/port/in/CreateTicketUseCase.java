package com.findu.help.v2.application.port.in;

import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.domain.model.ticket.HelpTicket;

public interface CreateTicketUseCase {
    HelpTicket execute(CreateTicketCommand command);
}

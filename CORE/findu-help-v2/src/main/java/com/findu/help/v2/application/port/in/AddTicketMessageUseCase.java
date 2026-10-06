package com.findu.help.v2.application.port.in;

import com.findu.help.v2.application.port.in.command.AddTicketMessageCommand;
import com.findu.help.v2.domain.model.ticket.TicketMessage;

public interface AddTicketMessageUseCase {
    TicketMessage execute(AddTicketMessageCommand command);
}

package com.findu.help.v2.application.port.in;

import com.findu.help.v2.domain.model.ticket.TicketMessage;

import java.util.List;

public interface GetTicketChatHistoryUseCase {
    List<TicketMessage> execute(String ticketCode);
}

package com.findu.help.v2.application.port.in;

import com.findu.help.v2.domain.model.ticket.HelpTicket;

import java.util.Optional;

public interface GetTicketByCodeUseCase {
    Optional<HelpTicket> execute(String ticketCode);
}

package com.findu.help.v2.application.port.out.persistence;

import com.findu.help.v2.application.port.in.GetTicketsUseCase;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.TicketCode;

import java.util.List;
import java.util.Optional;

public interface HelpTicketRepository {
    HelpTicket save(HelpTicket ticket);
    Optional<HelpTicket> findById(String id);
    Optional<HelpTicket> findByTicketCode(TicketCode ticketCode);
    Optional<HelpTicket> findByExternalRef(String externalRef);
    List<HelpTicket> findByFilter(GetTicketsUseCase.FilterQuery query);
}

package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.GetTicketChatHistoryUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.model.ticket.TicketMessage;
import com.findu.help.v2.domain.valueobject.TicketCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTicketChatHistoryService implements GetTicketChatHistoryUseCase {

    private final HelpTicketRepository helpTicketRepository;

    @Override
    public List<TicketMessage> execute(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new IllegalArgumentException("El código del ticket es obligatorio");
        }

        HelpTicket ticket = helpTicketRepository.findByTicketCode(new TicketCode(ticketCode))
                .or(() -> helpTicketRepository.findById(ticketCode))
                .orElseThrow(() -> new TicketNotFoundException("Ticket no encontrado con el código o id: " + ticketCode));

        return ticket.getMessages();
    }
}

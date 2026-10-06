package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.GetTicketByCodeUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.TicketCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetTicketByCodeService implements GetTicketByCodeUseCase {

    private final HelpTicketRepository helpTicketRepository;

    @Override
    public Optional<HelpTicket> execute(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            return Optional.empty();
        }

        return helpTicketRepository.findByTicketCode(new TicketCode(ticketCode))
                .or(() -> helpTicketRepository.findById(ticketCode));
    }
}

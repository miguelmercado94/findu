package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.GetTicketsUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTicketsService implements GetTicketsUseCase {

    private final HelpTicketRepository helpTicketRepository;

    @Override
    public List<HelpTicket> execute(FilterQuery query) {
        return helpTicketRepository.findByFilter(query);
    }
}

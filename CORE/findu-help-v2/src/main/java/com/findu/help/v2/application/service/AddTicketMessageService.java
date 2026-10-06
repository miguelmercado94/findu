package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.AddTicketMessageUseCase;
import com.findu.help.v2.application.port.in.command.AddTicketMessageCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.exception.TicketBusinessException;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.model.ticket.TicketMessage;
import com.findu.help.v2.domain.valueobject.TicketCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddTicketMessageService implements AddTicketMessageUseCase {

    private final HelpTicketRepository helpTicketRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    public TicketMessage execute(AddTicketMessageCommand command) {
        if (command == null || command.getTicketId() == null || command.getTicketId().isBlank()) {
            throw new IllegalArgumentException("El comando y el id del ticket son obligatorios");
        }

        HelpTicket ticket = helpTicketRepository.findById(command.getTicketId())
                .or(() -> helpTicketRepository.findByTicketCode(new TicketCode(command.getTicketId())))
                .orElseThrow(() -> new TicketNotFoundException("Ticket no encontrado con identificador: " + command.getTicketId()));

        TicketMessage addedMessage;
        try {
            addedMessage = ticket.addMessage(
                    command.getSenderId(),
                    command.getSenderType(),
                    command.getSenderName(),
                    command.getContent(),
                    command.getIsInternalNote(),
                    command.getAttachments()
            );
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new TicketBusinessException("No se pudo agregar el mensaje al ticket: " + e.getMessage(), e);
        }

        HelpTicket savedTicket = helpTicketRepository.save(ticket);

        for (DomainEvent event : savedTicket.pullUncommittedEvents()) {
            realtimeEventPublisher.publishRealtime(event);
        }

        return addedMessage;
    }
}

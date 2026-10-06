package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.ChangeTicketStatusUseCase;
import com.findu.help.v2.application.port.in.command.ChangeTicketStatusCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.ResolutionActionType;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.exception.TicketBusinessException;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.TicketCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangeTicketStatusService implements ChangeTicketStatusUseCase {

    private final HelpTicketRepository helpTicketRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    public HelpTicket execute(ChangeTicketStatusCommand command) {
        if (command == null || command.getTicketReference() == null || command.getTicketReference().isBlank()) {
            throw new IllegalArgumentException("El comando de cambio de estado y la referencia del ticket son obligatorios");
        }

        HelpTicket ticket = helpTicketRepository.findByTicketCode(new TicketCode(command.getTicketReference()))
                .or(() -> helpTicketRepository.findById(command.getTicketReference()))
                .orElseThrow(() -> new TicketNotFoundException("Ticket no encontrado con referencia: " + command.getTicketReference()));

        try {
            TicketStatus targetStatus = command.getNewStatus();
            if (targetStatus == null) {
                throw new IllegalArgumentException("El nuevo estado es obligatorio");
            }

            switch (targetStatus) {
                case ASIGNADO -> ticket.assignTo(command.getAssignedAgent());
                case RESUELTO -> ticket.resolve(ResolutionActionType.SOLO_INFORMACION, null, command.getReason());
                case CERRADO -> {
                    Long closedBy = command.getAssignedAgent() != null ? command.getAssignedAgent().getAgentId() : ticket.getRequester().getUserId();
                    ticket.close(closedBy);
                }
                default -> throw new TicketBusinessException("Transición de estado no soportada hacia: " + targetStatus);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new TicketBusinessException("No se pudo cambiar el estado del ticket: " + e.getMessage(), e);
        }

        HelpTicket savedTicket = helpTicketRepository.save(ticket);

        for (DomainEvent event : savedTicket.pullUncommittedEvents()) {
            realtimeEventPublisher.publishRealtime(event);
        }

        return savedTicket;
    }
}

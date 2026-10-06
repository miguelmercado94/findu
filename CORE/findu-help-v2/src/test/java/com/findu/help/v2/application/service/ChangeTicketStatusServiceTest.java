package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.command.ChangeTicketStatusCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.AssignedAgent;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeTicketStatusServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private ChangeTicketStatusService changeTicketStatusService;

    private HelpTicket sampleTicket;

    @BeforeEach
    void setUp() {
        sampleTicket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );
        // Limpiar evento inicial de creación
        sampleTicket.pullUncommittedEvents();
    }

    @Test
    @DisplayName("Debe cambiar el estado del ticket a ASIGNADO cuando se asigna un agente")
    void execute_AssignAgent_Success() {
        when(helpTicketRepository.findByTicketCode(any(TicketCode.class))).thenReturn(Optional.of(sampleTicket));
        when(helpTicketRepository.save(any(HelpTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangeTicketStatusCommand command = ChangeTicketStatusCommand.builder()
                .ticketReference(sampleTicket.getTicketCode().getValue())
                .newStatus(TicketStatus.ASIGNADO)
                .assignedAgent(new AssignedAgent(200L, "Agente Maria", "maria@findu.com"))
                .reason("Asignación de soporte")
                .build();

        HelpTicket updatedTicket = changeTicketStatusService.execute(command);

        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(updatedTicket.getAssignedAgent()).isNotNull();

        verify(helpTicketRepository, times(1)).save(any(HelpTicket.class));
        verify(realtimeEventPublisher, times(1)).publishRealtime(any(DomainEvent.class));
    }
}

package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.command.AddTicketMessageCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.model.ticket.TicketMessage;
import com.findu.help.v2.domain.valueobject.Location;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddTicketMessageServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private AddTicketMessageService addTicketMessageService;

    private HelpTicket existingTicket;

    @BeforeEach
    void setUp() {
        existingTicket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );
        // Limpiar evento inicial de creación
        existingTicket.pullUncommittedEvents();
    }

    @Test
    @DisplayName("Debe agregar mensaje al ticket existente y publicar evento en tiempo real")
    void execute_Success() {
        when(helpTicketRepository.findById(existingTicket.getId())).thenReturn(Optional.of(existingTicket));
        when(helpTicketRepository.save(any(HelpTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddTicketMessageCommand command = AddTicketMessageCommand.builder()
                .ticketId(existingTicket.getId())
                .senderId(100L)
                .senderType(SenderType.USUARIO)
                .senderName("Juan")
                .content("Hola, sigo con dudas")
                .isInternalNote(false)
                .attachments(Collections.emptyList())
                .build();

        TicketMessage addedMessage = addTicketMessageService.execute(command);

        assertThat(addedMessage).isNotNull();
        assertThat(addedMessage.getContent()).isEqualTo("Hola, sigo con dudas");

        verify(helpTicketRepository, times(1)).save(any(HelpTicket.class));
        verify(realtimeEventPublisher, times(1)).publishRealtime(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Debe lanzar TicketNotFoundException si el ticket no existe")
    void execute_TicketNotFound_ThrowsException() {
        when(helpTicketRepository.findById("invalid-id")).thenReturn(Optional.empty());

        AddTicketMessageCommand command = AddTicketMessageCommand.builder()
                .ticketId("invalid-id")
                .senderId(100L)
                .senderType(SenderType.USUARIO)
                .senderName("Juan")
                .content("Hola")
                .isInternalNote(false)
                .attachments(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> addTicketMessageService.execute(command))
                .isInstanceOf(TicketNotFoundException.class);
    }
}

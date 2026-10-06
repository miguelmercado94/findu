package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.model.ticket.TicketMessage;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketCode;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTicketChatHistoryServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @InjectMocks
    private GetTicketChatHistoryService getTicketChatHistoryService;

    @Test
    @DisplayName("Debe retornar los mensajes de chat del ticket")
    void execute_Success() {
        HelpTicket ticket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );

        when(helpTicketRepository.findByTicketCode(any(TicketCode.class))).thenReturn(Optional.of(ticket));

        List<TicketMessage> messages = getTicketChatHistoryService.execute(ticket.getTicketCode().getValue());

        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getContent()).isEqualTo("Descripcion");
    }
}

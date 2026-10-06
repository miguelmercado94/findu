package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.GetTicketsUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTicketsServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @InjectMocks
    private GetTicketsService getTicketsService;

    @Test
    @DisplayName("Debe consultar tickets filtrados desde el repositorio")
    void execute_Success() {
        HelpTicket ticket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );

        when(helpTicketRepository.findByFilter(any(GetTicketsUseCase.FilterQuery.class))).thenReturn(Collections.singletonList(ticket));

        GetTicketsUseCase.FilterQuery filter = GetTicketsUseCase.FilterQuery.builder().userId(100L).build();
        List<HelpTicket> tickets = getTicketsService.execute(filter);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getRequester().getUserId()).isEqualTo(100L);
    }
}

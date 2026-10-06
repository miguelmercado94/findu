package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTicketByCodeServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @InjectMocks
    private GetTicketByCodeService getTicketByCodeService;

    @Test
    @DisplayName("Debe retornar el ticket cuando existe por ticketCode")
    void execute_Success() {
        HelpTicket ticket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Asunto", "Descripcion", new Location("CO", "ANT", "MED"),
                Collections.emptyList(), null
        );

        when(helpTicketRepository.findByTicketCode(any(TicketCode.class))).thenReturn(Optional.of(ticket));

        Optional<HelpTicket> result = getTicketByCodeService.execute(ticket.getTicketCode().getValue());

        assertThat(result).isPresent();
        assertThat(result.get().getTicketCode()).isEqualTo(ticket.getTicketCode());
    }

    @Test
    @DisplayName("Debe retornar vacio si el ticketCode no existe")
    void execute_NotFound_ReturnsEmpty() {
        when(helpTicketRepository.findByTicketCode(any(TicketCode.class))).thenReturn(Optional.empty());

        Optional<HelpTicket> result = getTicketByCodeService.execute("HT-NONEXISTENT");

        assertThat(result).isEmpty();
    }
}

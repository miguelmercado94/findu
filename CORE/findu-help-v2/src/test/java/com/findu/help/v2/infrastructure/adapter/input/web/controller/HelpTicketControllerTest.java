package com.findu.help.v2.infrastructure.adapter.input.web.controller;

import com.findu.help.v2.application.port.in.*;
import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.application.port.out.storage.FileStoragePort;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.CreateTicketRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.request.LocationRequest;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.response.HelpTicketResponse;
import com.findu.help.v2.infrastructure.adapter.input.web.mapper.HelpTicketWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpTicketControllerTest {

    @Mock private CreateTicketUseCase createTicketUseCase;
    @Mock private ChangeTicketStatusUseCase changeTicketStatusUseCase;
    @Mock private GetTicketsUseCase getTicketsUseCase;
    @Mock private GetTicketChatHistoryUseCase getTicketChatHistoryUseCase;
    @Mock private AddTicketMessageUseCase addTicketMessageUseCase;
    @Mock private GetTicketByCodeUseCase getTicketByCodeUseCase;

    @Mock private FileStoragePort fileStoragePort;
    @Mock private HelpTicketWebMapper webMapper;

    @InjectMocks
    private HelpTicketController helpTicketController;

    @Test
    @DisplayName("Debe crear un ticket llamando a CreateTicketUseCase y devolviendo 201 CREATED")
    void createTicket_Success() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setUserId(100L);
        request.setUserRole(UserRole.CLIENTE);
        request.setSubject("Falla App");
        request.setDescription("No me abre");
        request.setLocation(new LocationRequest("Colombia", "Antioquia", "Medellin"));

        HelpTicket ticket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Falla App", "No me abre", new Location("Colombia", "Antioquia", "Medellin"),
                Collections.emptyList(), null
        );

        HelpTicketResponse responseDto = new HelpTicketResponse();
        responseDto.setId(ticket.getId());
        responseDto.setTicketCode(ticket.getTicketCode().getValue());
        responseDto.setStatus(TicketStatus.ABIERTO);

        when(webMapper.toCommand(any(CreateTicketRequest.class))).thenReturn(CreateTicketCommand.builder().build());
        when(createTicketUseCase.execute(any(CreateTicketCommand.class))).thenReturn(ticket);
        when(webMapper.toResponse(ticket)).thenReturn(responseDto);

        ResponseEntity<HelpTicketResponse> response = helpTicketController.createTicket(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTicketCode()).isEqualTo(ticket.getTicketCode().getValue());
    }

    @Test
    @DisplayName("Debe obtener un ticket por código y retornar 200 OK")
    void getTicketByCode_Success() {
        HelpTicket ticket = HelpTicket.create(
                new TicketRequester(100L, UserRole.CLIENTE, "Juan", "juan@example.com"),
                500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA,
                "Falla App", "No me abre", new Location("Colombia", "Antioquia", "Medellin"),
                Collections.emptyList(), null
        );

        HelpTicketResponse responseDto = new HelpTicketResponse();
        responseDto.setTicketCode(ticket.getTicketCode().getValue());

        when(getTicketByCodeUseCase.execute(ticket.getTicketCode().getValue())).thenReturn(Optional.of(ticket));
        when(webMapper.toResponse(ticket)).thenReturn(responseDto);

        ResponseEntity<HelpTicketResponse> response = helpTicketController.getTicketByCode(ticket.getTicketCode().getValue());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTicketCode()).isEqualTo(ticket.getTicketCode().getValue());
    }
}

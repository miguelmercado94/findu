package com.findu.help.v2.infrastructure.adapter.input.web.exception;

import com.findu.help.v2.domain.exception.TicketBusinessException;
import com.findu.help.v2.domain.exception.TicketNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Debe retornar 404 NOT_FOUND al capturar TicketNotFoundException")
    void handleTicketNotFound_Success() {
        when(request.getRequestURI()).thenReturn("/api/v1/help/tickets/HT-999");
        TicketNotFoundException ex = new TicketNotFoundException("Ticket no encontrado con codigo HT-999");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTicketNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("Ticket no encontrado");
    }

    @Test
    @DisplayName("Debe retornar 409 CONFLICT al capturar TicketBusinessException")
    void handleTicketBusinessException_Success() {
        when(request.getRequestURI()).thenReturn("/api/v1/help/tickets/HT-001/status");
        TicketBusinessException ex = new TicketBusinessException("Transición de estado no válida");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTicketBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Transición de estado no válida");
    }
}

package com.findu.help.v2.domain.model.ticket;

import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.ResolutionActionType;
import com.findu.help.v2.domain.enums.SenderType;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.event.TicketCreatedEvent;
import com.findu.help.v2.domain.valueobject.AssignedAgent;
import com.findu.help.v2.domain.valueobject.Location;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelpTicketTest {

    private TicketRequester createSampleRequester() {
        return new TicketRequester(100L, UserRole.CLIENTE, "Juan Perez", "juan@example.com");
    }

    private Location createSampleLocation() {
        return new Location("Colombia", "Antioquia", "Medellin");
    }

    @Test
    @DisplayName("Debe crear un ticket exitosamente con su estado inicial ABIERTO y evento de dominio registrado")
    void createTicket_Success() {
        TicketRequester requester = createSampleRequester();
        Location location = createSampleLocation();

        HelpTicket ticket = HelpTicket.create(
                requester,
                500L,
                "sub-123",
                "Problemas Técnicos",
                "Falla en App",
                PriorityLevel.ALTA,
                "Error en inicio de sesión",
                "No puedo ingresar con mi contraseña",
                location,
                Collections.emptyList(),
                null
        );

        assertThat(ticket).isNotNull();
        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getTicketCode()).isNotNull();
        assertThat(ticket.getTicketCode().getValue()).startsWith("HT-");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(ticket.getSubject()).isEqualTo("Error en inicio de sesión");
        assertThat(ticket.getDescription()).isEqualTo("No puedo ingresar con mi contraseña");
        assertThat(ticket.getMessages()).hasSize(1);
        assertThat(ticket.getStatusHistory()).hasSize(1);

        List<DomainEvent> events = ticket.pullUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TicketCreatedEvent.class);
        assertThat(ticket.pullUncommittedEvents()).isEmpty();
    }

    @Test
    @DisplayName("Debe lanzar excepción si falta información obligatoria en la creación")
    void createTicket_ValidationFailures() {
        TicketRequester requester = createSampleRequester();
        Location location = createSampleLocation();

        assertThatThrownBy(() -> HelpTicket.create(
                null, 500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA, "Subj", "Desc", location, Collections.emptyList(), null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("solicitante");

        assertThatThrownBy(() -> HelpTicket.create(
                requester, 500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA, "  ", "Desc", location, Collections.emptyList(), null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("asunto");

        assertThatThrownBy(() -> HelpTicket.create(
                requester, 500L, "sub-1", "Cat", "Sub", PriorityLevel.MEDIA, "Subj", "", location, Collections.emptyList(), null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("descripción");
    }

    @Test
    @DisplayName("Debe asignar ticket a un agente cuando está en estado ABIERTO")
    void assignTo_Success() {
        HelpTicket ticket = HelpTicket.create(
                createSampleRequester(), 500L, "sub-123", "Cat", "Sub", PriorityLevel.MEDIA, "Subject", "Description", createSampleLocation(), Collections.emptyList(), null
        );

        AssignedAgent agent = new AssignedAgent(200L, "Soporte Maria", "maria@findu.com");
        ticket.assignTo(agent);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(ticket.getAssignedAgent()).isEqualTo(agent);
        assertThat(ticket.getStatusHistory()).hasSize(2);
    }

    @Test
    @DisplayName("Debe fallar al asignar si el ticket no está en estado ABIERTO")
    void assignTo_Fails_IfNotAbierto() {
        HelpTicket ticket = HelpTicket.create(
                createSampleRequester(), 500L, "sub-123", "Cat", "Sub", PriorityLevel.MEDIA, "Subject", "Description", createSampleLocation(), Collections.emptyList(), null
        );
        AssignedAgent agent = new AssignedAgent(200L, "Soporte Maria", "maria@findu.com");
        ticket.assignTo(agent);

        // Ya está ASIGNADO
        assertThatThrownBy(() -> ticket.assignTo(agent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden tomar o asignar tickets en estado ABIERTO");
    }

    @Test
    @DisplayName("Debe agregar mensaje al chat del ticket")
    void addMessage_Success() {
        HelpTicket ticket = HelpTicket.create(
                createSampleRequester(), 500L, "sub-123", "Cat", "Sub", PriorityLevel.MEDIA, "Subject", "Description", createSampleLocation(), Collections.emptyList(), null
        );

        TicketMessage msg = ticket.addMessage(
                100L, SenderType.USUARIO, "Juan Perez", "Respuesta adicional", false, Collections.emptyList()
        );

        assertThat(msg).isNotNull();
        assertThat(ticket.getMessages()).hasSize(2);
        assertThat(ticket.getMessages().get(1).getContent()).isEqualTo("Respuesta adicional");
    }

    @Test
    @DisplayName("Debe resolver y cerrar el ticket siguiendo el ciclo de vida de estados")
    void resolveAndClose_Success() {
        HelpTicket ticket = HelpTicket.create(
                createSampleRequester(), 500L, "sub-123", "Cat", "Sub", PriorityLevel.MEDIA, "Subject", "Description", createSampleLocation(), Collections.emptyList(), null
        );
        AssignedAgent agent = new AssignedAgent(200L, "Soporte Maria", "maria@findu.com");
        ticket.assignTo(agent);

        ticket.resolve(ResolutionActionType.REEMBOLSO_PARCIAL, new BigDecimal("15.50"), "Solucionado satisfactoriamente");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESUELTO);
        assertThat(ticket.getResolution()).isNotNull();

        ticket.close(100L);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CERRADO);
        assertThat(ticket.getClosedAt()).isNotNull();

        // No se puede agregar mensaje si está CERRADO
        assertThatThrownBy(() -> ticket.addMessage(100L, SenderType.USUARIO, "Juan", "Texto", false, Collections.emptyList()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pueden agregar mensajes a un ticket CERRADO");
    }
}

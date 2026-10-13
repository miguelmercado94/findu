package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.TicketStatus;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.domain.model.category.HelpSubcategory;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.Location;
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
class CreateTicketServiceTest {

    @Mock
    private HelpTicketRepository helpTicketRepository;

    @Mock
    private HelpCategoryRepository helpCategoryRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private CreateTicketService createTicketService;

    private CreateTicketCommand sampleCommand;

    @BeforeEach
    void setUp() {
        sampleCommand = CreateTicketCommand.builder()
                .userId(100L)
                .userRole(UserRole.CLIENTE)
                .solicitudId(500L)
                .categoryId("cat-1")
                .subcategoryId("sub-1")
                .subject("Problema de pago")
                .description("No se procesó mi pago")
                .location(new Location("Colombia", "Antioquia", "Medellin"))
                .initialAttachments(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Debe crear un ticket guardándolo en repositorio y publicando evento en tiempo real")
    void execute_Success() {
        HelpCategory category = HelpCategory.builder()
                .id("cat-1")
                .name("Pagos")
                .code("CAT_PAGOS")
                .description("Categoría de Pagos")
                .targetRole(UserRole.CLIENTE)
                .order(1)
                .isActive(true)
                .subcategories(Collections.singletonList(
                        new HelpSubcategory("sub-1", "Error de Transacción", "Fallo al pagar", false, PriorityLevel.ALTA, 30, true)
                ))
                .build();

        when(helpCategoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(helpTicketRepository.save(any(HelpTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpTicket createdTicket = createTicketService.execute(sampleCommand);

        assertThat(createdTicket).isNotNull();
        assertThat(createdTicket.getSubject()).isEqualTo("Problema de pago");
        assertThat(createdTicket.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(createdTicket.getCategoryName()).isEqualTo("Pagos");
        assertThat(createdTicket.getSubcategoryName()).isEqualTo("Error de Transacción");

        verify(helpTicketRepository, times(1)).save(any(HelpTicket.class));
        verify(realtimeEventPublisher, times(1)).publishRealtime(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Debe ser idempotente: si ya existe un ticket con el mismo externalRef, lo retorna sin guardar ni publicar de nuevo")
    void execute_IdempotentOnExternalRef() {
        CreateTicketCommand activationCommand = CreateTicketCommand.builder()
                .userId(777L)
                .userRole(UserRole.PROVEEDOR)
                .externalRef("ACT-777")
                .subject("Validación de proveedor")
                .description("Revisión final de activación de proveedor")
                .initialAttachments(Collections.emptyList())
                .build();

        // Primera llamada: no existe -> crea, guarda y publica.
        when(helpTicketRepository.findByExternalRef("ACT-777")).thenReturn(Optional.empty());
        when(helpTicketRepository.save(any(HelpTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpTicket firstTicket = createTicketService.execute(activationCommand);
        assertThat(firstTicket).isNotNull();
        assertThat(firstTicket.getExternalRef()).isEqualTo("ACT-777");

        // Segunda llamada (retry): ya existe -> retorna el existente, sin volver a guardar ni publicar.
        when(helpTicketRepository.findByExternalRef("ACT-777")).thenReturn(Optional.of(firstTicket));

        HelpTicket secondTicket = createTicketService.execute(activationCommand);
        assertThat(secondTicket).isSameAs(firstTicket);

        // A través de ambas llamadas, save y publishRealtime se invocan exactamente una vez.
        verify(helpTicketRepository, times(1)).save(any(HelpTicket.class));
        verify(realtimeEventPublisher, times(1)).publishRealtime(any(DomainEvent.class));
    }
}

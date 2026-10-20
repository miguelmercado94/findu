package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.CreateTicketUseCase;
import com.findu.help.v2.application.port.in.command.CreateTicketCommand;
import com.findu.help.v2.application.port.out.event.RealtimeEventPublisher;
import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.application.port.out.persistence.HelpTicketRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.event.DomainEvent;
import com.findu.help.v2.domain.exception.TicketBusinessException;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.domain.model.category.HelpSubcategory;
import com.findu.help.v2.domain.model.priority.FixedPriorityCalculator;
import com.findu.help.v2.domain.model.ticket.HelpTicket;
import com.findu.help.v2.domain.valueobject.TicketRequester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateTicketService implements CreateTicketUseCase {

    private final HelpTicketRepository helpTicketRepository;
    private final HelpCategoryRepository helpCategoryRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    public HelpTicket execute(CreateTicketCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de creación no puede ser nulo");
        }

        // Idempotencia: si viene una referencia externa y ya existe un ticket con ella,
        // se retorna el ticket existente sin crear uno nuevo ni publicar eventos.
        if (command.getExternalRef() != null && !command.getExternalRef().isBlank()) {
            Optional<HelpTicket> existing = helpTicketRepository.findByExternalRef(command.getExternalRef());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        String categoryName = "";
        String subcategoryName = "";
        PriorityLevel priority = PriorityLevel.MEDIA;

        if (command.getCategoryId() != null && !command.getCategoryId().isBlank()) {
            Optional<HelpCategory> categoryOpt = helpCategoryRepository.findById(command.getCategoryId());
            if (categoryOpt.isPresent()) {
                HelpCategory category = categoryOpt.get();
                categoryName = category.getName();

                if (command.getSubcategoryId() != null && !command.getSubcategoryId().isBlank()) {
                    Optional<HelpSubcategory> subcategoryOpt = category.getSubcategories().stream()
                            .filter(sub -> sub.getSubcategoryId().equals(command.getSubcategoryId()))
                            .findFirst();

                    if (subcategoryOpt.isPresent()) {
                        HelpSubcategory subcategory = subcategoryOpt.get();
                        subcategoryName = subcategory.getName();
                        if (subcategory.getBasePriority() != null) {
                            priority = subcategory.getBasePriority();
                        }

                        // solicitudId es opcional: si el usuario lo tiene lo asocia, si no, se permite abrir el ticket de soporte
                        if (Boolean.TRUE.equals(subcategory.getRequiresSolicitudId()) && command.getSolicitudId() == null) {
                            log.debug("Ticket creado para subcategoría {} sin solicitudId asociado", subcategory.getName());
                        }
                    }
                }
            }
        }

        String requesterName = (command.getUserName() != null && !command.getUserName().isBlank())
                ? command.getUserName().trim()
                : (command.getUserRole() + "_" + command.getUserId());
        String requesterEmail = (command.getUserEmail() != null && !command.getUserEmail().isBlank())
                ? command.getUserEmail().trim()
                : ("user" + command.getUserId() + "@findu.com");

        TicketRequester requester = new TicketRequester(
                command.getUserId(),
                command.getUserRole(),
                requesterName,
                requesterEmail
        );

        HelpTicket ticket = HelpTicket.create(
                requester,
                command.getSolicitudId(),
                command.getSubcategoryId(),
                categoryName,
                subcategoryName,
                priority,
                command.getSubject(),
                command.getDescription(),
                command.getLocation(),
                command.getInitialAttachments(),
                new FixedPriorityCalculator()
        );

        if (command.getExternalRef() != null && !command.getExternalRef().isBlank()) {
            ticket.assignExternalRef(command.getExternalRef());
        }

        List<DomainEvent> events = ticket.pullUncommittedEvents();
        HelpTicket savedTicket = helpTicketRepository.save(ticket);

        for (DomainEvent event : events) {
            realtimeEventPublisher.publishRealtime(event);
        }

        return savedTicket;
    }
}

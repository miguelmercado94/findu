package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.externalapi.SupportTicketPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.usecase.ActivationProgressService;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Implementación de la recomputación de activación. Dueña de los invariantes de orden:
 * el ticket a soporte solo se dispara cuando {@code otp_ok && email_ok && biometria_ok} y
 * {@code soporte_notified == false}; en 2xx fija {@code soporte_notified=true} + ticket_code y
 * avanza a {@code EN_REVISION_SOPORTE}; si help-v2 falla (5xx) deja la guarda en false y loguea
 * ERROR (reintento por scheduler). Idempotente en llamadas repetidas.
 */
@Service
public class ActivationProgressServiceImpl implements ActivationProgressService {

    private static final Logger log = LoggerFactory.getLogger(ActivationProgressServiceImpl.class);
    private static final String TICKET_SUBJECT = "Revisión de activación de proveedor";

    private final ProviderActivationRepositoryPort activationRepository;
    private final SupportTicketPort supportTicketPort;

    public ActivationProgressServiceImpl(ProviderActivationRepositoryPort activationRepository,
                                         SupportTicketPort supportTicketPort) {
        this.activationRepository = activationRepository;
        this.supportTicketPort = supportTicketPort;
    }

    @Override
    public Mono<ProviderActivation> recompute(Long userId) {
        return activationRepository.loadByUserId(userId)
                .flatMap(this::progress);
    }

    private Mono<ProviderActivation> progress(ProviderActivation activation) {
        // Estados terminales / ya en revisión: nada que avanzar aquí.
        if (activation.getEstado() == ProviderActivationState.ACTIVADO
                || activation.getEstado() == ProviderActivationState.RECHAZADO) {
            return Mono.just(activation);
        }
        if (!activation.validacionesPreviasCompletas()) {
            // Aún faltan validaciones: asegurar estado PENDIENTE_VALIDACIONES.
            if (activation.getEstado() != ProviderActivationState.PENDIENTE_VALIDACIONES) {
                return persistState(activation, ProviderActivationState.PENDIENTE_VALIDACIONES,
                        activation.isSoporteNotified(), activation.getSoporteTicketCode());
            }
            return Mono.just(activation);
        }
        // Las tres previas completas.
        if (activation.isSoporteNotified()) {
            // Ya se notificó: no-op (idempotente). Garantizamos estado EN_REVISION_SOPORTE.
            if (activation.getEstado() != ProviderActivationState.EN_REVISION_SOPORTE) {
                return persistState(activation, ProviderActivationState.EN_REVISION_SOPORTE,
                        true, activation.getSoporteTicketCode());
            }
            return Mono.just(activation);
        }
        return fireSupportTicket(activation);
    }

    private Mono<ProviderActivation> fireSupportTicket(ProviderActivation activation) {
        Long userId = activation.getUserId();
        String externalRef = "ACT-" + userId;
        String description = "El proveedor (userId=" + userId + ") completó OTP, correo y biométrica. "
                + "Validar los datos y aprobar o rechazar la activación.";
        return supportTicketPort.createActivationTicket(userId, TICKET_SUBJECT, description, externalRef)
                .flatMap(ticketCode -> {
                    activation.setSoporteNotified(true);
                    activation.setSoporteTicketCode(ticketCode);
                    return persistState(activation, ProviderActivationState.EN_REVISION_SOPORTE, true, ticketCode);
                })
                .onErrorResume(e -> {
                    // help-v2 5xx/timeout: verdad local avanza, guarda en false; reintento por scheduler.
                    log.error("No se pudo crear el ticket de soporte (userId={}): {}. Se reintentará.",
                            userId, e.getMessage());
                    return persistState(activation, ProviderActivationState.EN_REVISION_SOPORTE, false, null);
                });
    }

    private Mono<ProviderActivation> persistState(ProviderActivation activation, ProviderActivationState estado,
                                                  boolean soporteNotified, String ticketCode) {
        activation.setEstado(estado);
        activation.setSoporteNotified(soporteNotified);
        activation.setSoporteTicketCode(ticketCode);
        return activationRepository.updateUserActivationState(activation.getUserId(), estado.name(), soporteNotified, ticketCode)
                .thenReturn(activation);
    }
}

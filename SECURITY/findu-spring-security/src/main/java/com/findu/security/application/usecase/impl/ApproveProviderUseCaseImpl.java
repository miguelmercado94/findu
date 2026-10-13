package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.usecase.ApproveProviderUseCase;
import com.findu.security.domain.model.ProviderActivation;
import com.findu.security.domain.model.ProviderActivationState;
import com.findu.security.domain.model.ValidationState;
import com.findu.security.domain.model.ValidationType;
import com.findu.security.dto.response.ActivationStatusResponse;
import com.findu.security.util.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Aprobación por soporte. Precondición: {@code EN_REVISION_SOPORTE} (si no, 409). Marca ACTIVADO,
 * inserta validación SOPORTE OK y despacha PROVEEDOR_ACTIVADO (presencia) con recipient
 * {@code PROVEEDOR_{userId}}. El despacho es no-fatal.
 */
@Service
public class ApproveProviderUseCaseImpl implements ApproveProviderUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApproveProviderUseCaseImpl.class);

    private final ProviderActivationRepositoryPort activationRepository;
    private final NotificationPort notificationPort;

    public ApproveProviderUseCaseImpl(ProviderActivationRepositoryPort activationRepository,
                                      NotificationPort notificationPort) {
        this.activationRepository = activationRepository;
        this.notificationPort = notificationPort;
    }

    @Override
    public Mono<ActivationStatusResponse> approve(Long userId) {
        return activationRepository.loadByUserId(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "proveedor_no_encontrado")))
                .flatMap(activation -> {
                    if (activation.getEstado() != ProviderActivationState.EN_REVISION_SOPORTE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "estado_invalido"));
                    }
                    return activationRepository.upsertValidation(userId, ValidationType.SOPORTE, ValidationState.OK, null)
                            .then(activationRepository.updateUserActivationState(userId,
                                    ProviderActivationState.ACTIVADO.name(), activation.isSoporteNotified(),
                                    activation.getSoporteTicketCode()))
                            .then(dispatch(userId))
                            .then(activationRepository.loadByUserId(userId))
                            .map(ActivationStatusResponse::from);
                });
    }

    private Mono<Void> dispatch(Long userId) {
        return notificationPort.send("NOTIFICACION", "PROVEEDOR_" + userId,
                        NotificationTemplates.PROVEEDOR_ACTIVADO, "es", Map.of())
                .onErrorResume(e -> {
                    log.warn("Despacho PROVEEDOR_ACTIVADO falló (userId={}, no-fatal): {}", userId, e.getMessage());
                    return Mono.empty();
                });
    }
}

package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.NotificationPort;
import com.findu.security.application.port.output.persistence.ProviderActivationRepositoryPort;
import com.findu.security.application.usecase.RejectProviderUseCase;
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
 * Rechazo por soporte. Precondición: {@code EN_REVISION_SOPORTE} (si no, 409). Marca RECHAZADO,
 * inserta validación SOPORTE RECHAZADA (detalle=motivo), resetea {@code soporte_notified=false} para
 * permitir re-abrir la revisión en un re-envío, y despacha PROVEEDOR_RECHAZADO. El despacho es no-fatal.
 */
@Service
public class RejectProviderUseCaseImpl implements RejectProviderUseCase {

    private static final Logger log = LoggerFactory.getLogger(RejectProviderUseCaseImpl.class);

    private final ProviderActivationRepositoryPort activationRepository;
    private final NotificationPort notificationPort;

    public RejectProviderUseCaseImpl(ProviderActivationRepositoryPort activationRepository,
                                     NotificationPort notificationPort) {
        this.activationRepository = activationRepository;
        this.notificationPort = notificationPort;
    }

    @Override
    public Mono<ActivationStatusResponse> reject(Long userId, String motivo) {
        return activationRepository.loadByUserId(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "proveedor_no_encontrado")))
                .flatMap(activation -> {
                    if (activation.getEstado() != ProviderActivationState.EN_REVISION_SOPORTE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "estado_invalido"));
                    }
                    return activationRepository.upsertValidation(userId, ValidationType.SOPORTE, ValidationState.RECHAZADA, motivo)
                            .then(activationRepository.updateUserActivationState(userId,
                                    ProviderActivationState.RECHAZADO.name(), false, activation.getSoporteTicketCode()))
                            .then(dispatch(userId, motivo))
                            .then(activationRepository.loadByUserId(userId))
                            .map(ActivationStatusResponse::from);
                });
    }

    private Mono<Void> dispatch(Long userId, String motivo) {
        return notificationPort.send("NOTIFICACION", "PROVEEDOR_" + userId,
                        NotificationTemplates.PROVEEDOR_RECHAZADO, "es", Map.of("motivo", motivo))
                .onErrorResume(e -> {
                    log.warn("Despacho PROVEEDOR_RECHAZADO falló (userId={}, no-fatal): {}", userId, e.getMessage());
                    return Mono.empty();
                });
    }
}

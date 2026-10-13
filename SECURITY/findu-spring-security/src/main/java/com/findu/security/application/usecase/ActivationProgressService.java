package com.findu.security.application.usecase;

import com.findu.security.domain.model.ProviderActivation;
import reactor.core.publisher.Mono;

/**
 * Recomputa el estado de activación del proveedor tras cada validación y dispara (una sola vez)
 * el ticket de soporte cuando OTP+correo+biométrica están completas.
 */
public interface ActivationProgressService {

    /**
     * Recarga el agregado, y si las tres validaciones previas están completas y el ticket aún no
     * se notificó, crea el ticket en help-v2 y avanza a {@code EN_REVISION_SOPORTE}. Idempotente.
     *
     * @return el agregado tras la recomputación.
     */
    Mono<ProviderActivation> recompute(Long userId);
}

package com.findu.security.application.usecase;

import com.findu.security.dto.response.ActivationStatusResponse;
import reactor.core.publisher.Mono;

/**
 * Aprobación por soporte: precondición {@code EN_REVISION_SOPORTE} (si no, 409). Marca ACTIVADO,
 * inserta validación SOPORTE OK y despacha PROVEEDOR_ACTIVADO.
 */
public interface ApproveProviderUseCase {
    Mono<ActivationStatusResponse> approve(Long userId);
}

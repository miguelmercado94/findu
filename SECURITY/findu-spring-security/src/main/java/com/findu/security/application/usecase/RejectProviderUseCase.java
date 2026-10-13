package com.findu.security.application.usecase;

import com.findu.security.dto.response.ActivationStatusResponse;
import reactor.core.publisher.Mono;

/**
 * Rechazo por soporte: precondición {@code EN_REVISION_SOPORTE} (si no, 409). Marca RECHAZADO,
 * inserta validación SOPORTE RECHAZADA con el motivo, resetea {@code soporte_notified=false} y
 * despacha PROVEEDOR_RECHAZADO.
 */
public interface RejectProviderUseCase {
    Mono<ActivationStatusResponse> reject(Long userId, String motivo);
}

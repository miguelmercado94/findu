package com.findu.security.application.usecase;

import com.findu.security.dto.response.ActivationStatusResponse;
import reactor.core.publisher.Mono;

/**
 * Devuelve el estado de activación del proveedor autenticado (endpoint ligero GET /status).
 */
public interface GetActivationStatusUseCase {
    Mono<ActivationStatusResponse> getStatus();
}

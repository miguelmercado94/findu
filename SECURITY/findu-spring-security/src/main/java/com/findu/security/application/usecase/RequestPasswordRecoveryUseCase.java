package com.findu.security.application.usecase;

import com.findu.security.dto.request.ForgotPasswordRequest;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: solicitar recuperación de contraseña.
 * Busca usuario por email o teléfono, genera código de 6 dígitos, persiste y envía.
 * Por seguridad no revela si el usuario existe (si no existe, se responde igual).
 */
public interface RequestPasswordRecoveryUseCase {

    /**
     * Solicita la recuperación de contraseña.
     *
     * @param request Datos de la solicitud (email o teléfono)
     * @return Mono vacío
     */
    Mono<Void> requestRecovery(ForgotPasswordRequest request);
}

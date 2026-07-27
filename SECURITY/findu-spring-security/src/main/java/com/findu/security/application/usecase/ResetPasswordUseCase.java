package com.findu.security.application.usecase;

import com.findu.security.dto.request.ResetPasswordRequest;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: restablecer contraseña.
 */
public interface ResetPasswordUseCase {

    /**
     * Restablece la contraseña del usuario.
     *
     * @param request datos del restablecimiento (token o código de 6 dígitos con nueva contraseña)
     * @return Mono vacío si OK
     */
    Mono<Void> resetPassword(ResetPasswordRequest request);
}

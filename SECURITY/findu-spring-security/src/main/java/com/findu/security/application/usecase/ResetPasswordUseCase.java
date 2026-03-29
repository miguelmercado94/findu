package com.findu.security.application.usecase;

import reactor.core.publisher.Mono;

/**
 * Caso de uso: restablecer contraseña con el token recibido por correo.
 */
public interface ResetPasswordUseCase {

    /**
     * Restablece la contraseña del usuario asociado al token.
     *
     * @param token   token de recuperación (recibido por correo)
     * @param newPassword nueva contraseña en texto plano (se codificará con el encoder del sistema)
     * @return Mono vacío si OK; error si token inválido, expirado o ya usado
     */
    Mono<Void> resetPassword(String token, String newPassword);
}

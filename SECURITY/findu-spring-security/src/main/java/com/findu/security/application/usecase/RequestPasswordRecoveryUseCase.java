package com.findu.security.application.usecase;

import reactor.core.publisher.Mono;

/**
 * Caso de uso: solicitar recuperación de contraseña.
 * Busca usuario por email o username, genera token, persiste y envía correo con enlace.
 * Por seguridad no revela si el usuario existe (si no existe, se responde igual).
 */
public interface RequestPasswordRecoveryUseCase {

    /**
     * Solicita la recuperación de contraseña para el email o username indicado.
     *
     * @param emailOrUsername email o nombre de usuario
     * @return Mono vacío que completa cuando el flujo termina (siempre éxito desde el punto de vista del cliente)
     */
    Mono<Void> requestRecovery(String emailOrUsername);
}

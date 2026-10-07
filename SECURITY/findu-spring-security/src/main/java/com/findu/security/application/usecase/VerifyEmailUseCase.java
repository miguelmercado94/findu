package com.findu.security.application.usecase;

import reactor.core.publisher.Mono;

/**
 * Caso de uso: validar el token del enlace mágico de verificación de correo del proveedor.
 */
public interface VerifyEmailUseCase {

    /**
     * Valida el token (por hash, no usado y no expirado). Si es correcto, marca
     * email_verified=true, el token como usado y avanza account_status de
     * PENDING_EMAIL_VERIFICATION a DOCUMENTS_PENDING.
     *
     * @param token token en claro recibido en el enlace
     * @return true si la verificación fue exitosa; false en caso contrario
     */
    Mono<Boolean> verify(String token);
}

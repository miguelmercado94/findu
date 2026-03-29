package com.findu.security.application.port.output;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para envío de correos (recuperación de contraseña, etc.).
 */
public interface EmailSenderPort {

    /**
     * Envía un correo de recuperación de contraseña con el enlace que incluye el token.
     *
     * @param toEmail dirección de destino
     * @param resetLinkUrl URL completa del enlace de restablecimiento (incluye token)
     * @return Mono que completa cuando el envío se ha aceptado (o falla)
     */
    Mono<Void> sendPasswordRecoveryEmail(String toEmail, String resetLinkUrl);
}

package com.findu.security.application.usecase;

import com.findu.security.domain.model.Usuario;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: generar y enviar el enlace mágico de verificación de correo del proveedor.
 */
public interface SendEmailVerificationUseCase {

    /**
     * Genera un token criptográfico, persiste solo su hash con expiración y envía el
     * enlace de verificación por el microservicio de notificaciones. No revela si el
     * correo existe.
     *
     * @param email correo del proveedor
     * @return Mono vacío
     */
    Mono<Void> sendVerification(String email);

    /**
     * Igual que {@link #sendVerification(String)} pero reutilizando un usuario ya cargado
     * (usado en el flujo federado para disparar el correo sin re-consultar la BD).
     *
     * @param user usuario destino
     * @return Mono vacío
     */
    Mono<Void> sendVerificationForUser(Usuario user);
}

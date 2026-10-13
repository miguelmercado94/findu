package com.findu.security.application.port.output.externalapi;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Puerto de salida hacia findu-core: lee el perfil del proveedor para re-validar la biométrica
 * (nombre completo + fecha de nacimiento registrados) y propaga la foto frontal (best-effort).
 */
public interface CorePerfilPort {

    /**
     * Datos del perfil del proveedor en findu-core (vista mínima para la re-validación biométrica).
     *
     * @param id              id del perfil en core (NO el authUserId); usado en el PUT de la foto.
     * @param nombreCompleto  nombre completo registrado.
     * @param fechaNacimiento fecha de nacimiento registrada.
     * @param urlImagenPerfil foto de perfil actual (puede ser null).
     */
    record PerfilProveedor(Long id, String nombreCompleto, LocalDate fechaNacimiento, String urlImagenPerfil) {}

    /**
     * Obtiene el perfil del proveedor por authUserId de seguridad.
     * {@code Mono.empty()} si no existe aún (el caso 404 -> 409 perfil_core_requerido lo decide el use case).
     */
    Mono<PerfilProveedor> fetchByAuthUserId(Long authUserId);

    /**
     * Propaga best-effort la foto frontal a core vía PUT /perfil-proveedor/{perfilId}.
     * No es fatal: los errores se tragan (reconciliados por el scheduler).
     */
    Mono<Void> pushProfilePhoto(Long perfilId, String nombreCompleto, String urlImagenPerfil);
}

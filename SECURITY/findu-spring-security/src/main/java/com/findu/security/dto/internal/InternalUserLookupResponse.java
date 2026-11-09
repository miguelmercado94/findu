package com.findu.security.dto.internal;

/**
 * Respuesta del lookup interno por username. {@code proveedorId} es el id del perfil del
 * proveedor en findu-core (nullable si el usuario aún no tiene perfil).
 */
public record InternalUserLookupResponse(Long authUserId, String username, Long proveedorId) {
}

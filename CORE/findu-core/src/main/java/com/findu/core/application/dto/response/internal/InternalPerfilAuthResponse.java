package com.findu.core.application.dto.response.internal;

/** Resolucion interna authUserId -> perfil de proveedor (usada por el dispatcher para validar el canal del WebSocket). */
public record InternalPerfilAuthResponse(Long perfilId, Long authUserId) {
}

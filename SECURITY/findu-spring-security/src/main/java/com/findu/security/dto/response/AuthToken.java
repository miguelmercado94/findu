package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con tokens de autenticación (access + refresh).
 * Tras login/registro/refresh, {@code available} es {@code true}.
 * Tras logout, la API devuelve el mismo shape con {@code available false} (sin reenviar secretos en jwt/jwtRefresh).
 */
public record AuthToken(
    @Schema(description = "Access token JWT")
    String jwt,
    @Schema(description = "Refresh token JWT")
    String jwtRefresh,
    @Schema(description = "true mientras el par sea usable; false tras cerrar sesión (el cliente invalida su copia local)")
    boolean available
) {
    /** Respuesta de logout: sesión cerrada; no se devuelven los tokens en claro. */
    public static AuthToken loggedOut() {
        return new AuthToken("", "", false);
    }
}

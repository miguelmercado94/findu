package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Respuesta de validación de token (JWT o JWT Refresh).
 * No incluye la firma (signature) por seguridad.
 *
 * @param tokenValid true si el token es válido (firma correcta y no expirado)
 * @param header     header del token decodificado (solo si tokenValid es true)
 * @param payload    payload del token decodificado (solo si tokenValid es true)
 */
public record ValidateTokenResponse(
        @Schema(description = "Indica si el token es valido")
        boolean tokenValid,
        @Schema(description = "Header JWT decodificado")
        Map<String, Object> header,
        @Schema(description = "Payload JWT decodificado")
        Map<String, Object> payload
) {
    public static ValidateTokenResponse invalid() {
        return new ValidateTokenResponse(false, null, null);
    }
}

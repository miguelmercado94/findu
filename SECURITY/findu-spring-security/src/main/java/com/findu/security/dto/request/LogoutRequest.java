package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Cierre de sesión: el access JWT es obligatorio (es el que invalida la sesión en API).
 * El refresh es opcional; si se envía, también se marca revocado en almacén (DynamoDB / memoria).
 */
public record LogoutRequest(
        @NotBlank(message = "accessToken es requerido (JWT de acceso)")
        @Schema(description = "Access token JWT a revocar (obligatorio)", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,
        @Schema(description = "Refresh token JWT (opcional; revocación adicional hasta caché dedicada)", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVFJlZnJlc2gifQ...")
        String refreshToken
) {}

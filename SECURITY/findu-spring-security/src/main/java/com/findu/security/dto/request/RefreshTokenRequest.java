package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para el endpoint de refresh: body con el refresh token.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "refreshToken es requerido")
    @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVFJlZnJlc2gifQ...")
    String refreshToken
) {}

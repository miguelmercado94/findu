package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitar recuperación de contraseña (email o username).
 */
public record ForgotPasswordRequest(
    @Schema(example = "lmarquez@example.com", description = "Email del usuario")
    String email,

    @Schema(example = "3001234567", description = "Teléfono móvil del usuario")
    String phone,

    @Schema(example = "+57", description = "Código internacional del país")
    String codPhoneInternational
) {}

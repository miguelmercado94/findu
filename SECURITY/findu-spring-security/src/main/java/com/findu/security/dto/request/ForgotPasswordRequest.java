package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitar recuperación de contraseña (email o username).
 */
public record ForgotPasswordRequest(

    @NotBlank(message = "email o nombre de usuario es requerido")
    @Schema(example = "lmarquez@example.com", description = "Email o username del usuario")
    String emailOrUsername
) {}

package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de envío del enlace mágico de verificación de correo del proveedor.
 */
public record SendEmailVerificationRequest(
        @Schema(example = "proveedor@ejemplo.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        String email
) {}

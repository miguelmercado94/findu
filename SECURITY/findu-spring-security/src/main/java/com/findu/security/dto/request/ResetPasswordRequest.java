package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Schema(example = "4f01f714266e4d7798f7af0f8e3f54d3", description = "Token de enlace de recuperación (opcional)")
    String token,

    @Schema(example = "123456", description = "Código de 6 dígitos de recuperación (opcional)")
    String code,

    @Schema(example = "lmarquez@example.com", description = "Email del usuario (requerido si se usa código)")
    String email,

    @Schema(example = "3001234567", description = "Teléfono del usuario (requerido si se usa código)")
    String phone,

    @Schema(example = "+57", description = "Código internacional del país (requerido si se usa teléfono)")
    String codPhoneInternational,

    @NotBlank(message = "nueva contraseña es requerida")
    @Size(min = 6, message = "la contraseña debe tener al menos 6 caracteres")
    @Schema(example = "NuevaPassword123*")
    String newPassword
) {}

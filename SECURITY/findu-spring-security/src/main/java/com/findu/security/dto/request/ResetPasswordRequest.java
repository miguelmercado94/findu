package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "token es requerido")
    @Schema(example = "4f01f714266e4d7798f7af0f8e3f54d3")
    String token,
    @NotBlank(message = "nueva contraseña es requerida")
    @Size(min = 6, message = "la contraseña debe tener al menos 6 caracteres")
    @Schema(example = "NuevaPassword123*")
    String newPassword
) {}

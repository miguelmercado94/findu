package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del POST /api/v1/activation/reject. El path es estático; el {@code userId} viaja aquí.
 */
public record RejectProviderRequest(
        @Schema(example = "42")
        @NotNull(message = "userId es obligatorio")
        @Positive(message = "userId debe ser positivo")
        Long userId,

        @Schema(example = "Los datos de la cédula no coinciden")
        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 500, message = "motivo excede 500 caracteres")
        String motivo
) {}

package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Cuerpo del POST /api/v1/activation/approve. El path es estático; el {@code userId} viaja aquí.
 */
public record ApproveProviderRequest(
        @Schema(example = "42")
        @NotNull(message = "userId es obligatorio")
        @Positive(message = "userId debe ser positivo")
        Long userId
) {}

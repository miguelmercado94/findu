package com.findu.core.dto.request;

import jakarta.validation.constraints.NotNull;

public record CrearEspecialidadRequest(
        @NotNull Long perfilProveedorId,
        @NotNull Long servicioId,
        String descripcion,
        Integer experienciaAnios
) {}

package com.findu.core.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearCalificacionRequest(
        @NotNull Long solicitudServicioId,
        @NotNull Long evaluadorId,
        @NotNull Long evaluadoId,
        @NotNull String tipoEvaluacion,
        @NotNull @Min(1) @Max(5) Integer puntaje,
        String comentario
) {}

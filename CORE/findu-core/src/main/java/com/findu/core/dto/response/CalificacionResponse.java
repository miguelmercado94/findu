package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de calificación")
public record CalificacionResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre del evaluador") String evaluadorNombre,
        @Schema(description = "Username del evaluador") String evaluadorUsername,
        @Schema(description = "Tipo de evaluación") String tipoEvaluacion,
        @Schema(description = "Puntaje") Integer puntaje,
        @Schema(description = "Comentario") String comentario,
        @Schema(description = "ID del servicio asociado a la solicitud calificada") Long servicioId,
        @Schema(description = "Nombre del servicio asociado a la solicitud calificada") String servicioNombre
) {}

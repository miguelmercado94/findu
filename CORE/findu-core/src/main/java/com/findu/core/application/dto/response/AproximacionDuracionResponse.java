package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.UnidadDuracionOferta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Detalle de la aproximación de duración de la oferta")
public record AproximacionDuracionResponse(
        @Schema(description = "Unidad de duración (MINUTOS, HORAS, DIAS)", example = "HORAS")
        UnidadDuracionOferta unidad,
        @Schema(description = "Cantidad en la unidad seleccionada", example = "1.00")
        BigDecimal cantidad,
        @Schema(description = "Indica si todos los días comparten el mismo horario (cuando unidad = DIAS)", example = "false")
        Boolean mismoHorarioTodosLosDias,
        @Schema(description = "Total de horas estimadas de trabajo sumando la duración o los días programados", example = "1.00")
        BigDecimal totalHorasEstimadas,
        @Schema(description = "Cronograma detallado por día cuando unidad = DIAS")
        List<DiaTrabajoOfertaResponse> cronogramaDias
) {
}


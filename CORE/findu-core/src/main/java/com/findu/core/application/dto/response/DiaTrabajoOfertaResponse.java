package com.findu.core.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Detalle del día de trabajo programado en la oferta")
public record DiaTrabajoOfertaResponse(
        @Schema(description = "ID del registro de cronograma") Long id,
        @Schema(description = "Fecha del día de trabajo", example = "2026-11-02")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fecha,
        @Schema(description = "Día de la semana", example = "LUNES") String diaSemana,
        @Schema(description = "Hora de inicio de la jornada", example = "08:00:00")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime horaInicio,
        @Schema(description = "Hora de fin de la jornada", example = "12:00:00")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime horaFin,
        @Schema(description = "Horas de trabajo programadas ese día", example = "4.00") BigDecimal horasTrabajo
) {
}


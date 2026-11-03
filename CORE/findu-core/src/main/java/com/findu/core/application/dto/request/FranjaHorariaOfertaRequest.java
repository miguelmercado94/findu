package com.findu.core.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "Franja horaria de trabajo dentro de un día (ej. 08:00 a 12:00, o 13:00 a 17:00)")
public record FranjaHorariaOfertaRequest(
        @Schema(description = "Hora de inicio de la franja (HH:mm o HH:mm:ss)", example = "08:00")
        @JsonAlias({"hora_inicio", "startTime"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaInicio,

        @Schema(description = "Hora de fin de la franja (HH:mm o HH:mm:ss)", example = "12:00")
        @JsonAlias({"hora_fin", "endTime"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaFin,

        @Schema(description = "Horas de trabajo en esta franja (opcional, se calcula de horaInicio y horaFin)", example = "4.0")
        @JsonAlias({"horas_trabajo", "horas"})
        BigDecimal horasTrabajo
) {
}


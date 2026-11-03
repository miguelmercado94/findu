package com.findu.core.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Detalle del horario de trabajo para un día específico cuando la duración de la oferta se expresa en DÍAS. " +
        "Soporta una franja directa (horaInicio/horaFin) o múltiples franjas en el mismo día (ej. 08:00-12:00 y 13:00-17:00).")
public record DiaTrabajoOfertaRequest(
        @NotNull(message = "La fecha del día de trabajo es obligatoria")
        @Schema(description = "Fecha del día de trabajo (formato YYYY-MM-DD)", example = "2026-10-27")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fecha,

        @Schema(description = "Día de la semana (opcional, se calcula automáticamente a partir de la fecha si no se envía)", example = "MARTES")
        @JsonAlias({"dia_semana", "dayOfWeek"})
        String diaSemana,

        @Schema(description = "Hora de inicio de trabajo en ese día (formato HH:mm o HH:mm:ss)", example = "08:00")
        @JsonAlias({"hora_inicio", "startTime"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaInicio,

        @Schema(description = "Hora de finalización de trabajo en ese día (formato HH:mm o HH:mm:ss)", example = "12:00")
        @JsonAlias({"hora_fin", "endTime"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaFin,

        @Schema(description = "Cantidad de horas a trabajar en ese día (si se envían horaInicio y horaFin, se calcula automáticamente)", example = "4.0")
        @JsonAlias({"horas_trabajo", "horas", "cantidadHoras"})
        BigDecimal horasTrabajo,

        @Valid
        @Schema(description = "Lista opcional de franjas horarias cuando un mismo día tiene múltiples bloques (ej. 08:00-12:00 y 13:00-17:00)")
        @JsonAlias({"franjas", "franjasHorarias", "franjas_horarias", "turnos", "bloques"})
        List<FranjaHorariaOfertaRequest> franjas
) {
    public DiaTrabajoOfertaRequest(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, BigDecimal horasTrabajo) {
        this(fecha, null, horaInicio, horaFin, horasTrabajo, null);
    }

    public DiaTrabajoOfertaRequest(LocalDate fecha, String diaSemana, LocalTime horaInicio, LocalTime horaFin, BigDecimal horasTrabajo) {
        this(fecha, diaSemana, horaInicio, horaFin, horasTrabajo, null);
    }
}

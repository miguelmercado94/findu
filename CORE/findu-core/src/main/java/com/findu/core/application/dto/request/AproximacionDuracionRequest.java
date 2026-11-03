package com.findu.core.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.findu.core.domain.enums.UnidadDuracionOferta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Schema(description = "Aproximación de duración del servicio propuesto en la oferta. Por defecto es 1 HORA. " +
        "Si la unidad es DIAS, debe especificar los días a trabajar y el horario/horas en cada día (o usar la opción rápida de mismo horario todos los días).")
public record AproximacionDuracionRequest(
        @Schema(description = "Unidad de tiempo: MINUTOS, HORAS (por defecto) o DIAS", example = "HORAS", defaultValue = "HORAS")
        @JsonAlias({"formato", "tipoUnidad", "unidad_duracion", "unit"})
        UnidadDuracionOferta unidad,

        @Schema(description = "Cantidad de unidades de tiempo (por defecto 1 cuando es HORAS)", example = "1")
        @JsonAlias({"valor", "duracion", "cantidad_duracion", "amount"})
        BigDecimal cantidad,

        @Schema(description = "Opción rápida: si es true, aplica horaInicioGeneral, horaFinGeneral y/o horasTrabajoPorDia a todos los días seleccionados", example = "false")
        @JsonAlias({"mismoHorario", "mismo_horario_todos_los_dias", "mismoHorarioDias"})
        Boolean mismoHorarioTodosLosDias,

        @Schema(description = "Fecha inicial del rango cuando se usa generación rápida por rango de fechas (ej. 2026-11-02)", example = "2026-11-02")
        @JsonAlias({"fecha_inicio", "startDate"})
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fechaInicio,

        @Schema(description = "Fecha final del rango cuando se usa generación rápida por rango de fechas (ej. 2026-11-04)", example = "2026-11-04")
        @JsonAlias({"fecha_fin", "endDate"})
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fechaFin,

        @Schema(description = "Filtro opcional de días de la semana dentro del rango (ej. [\"LUNES\", \"MARTES\", \"MIERCOLES\"])")
        @JsonAlias({"diasSemana", "dias_semana"})
        Set<String> diasSemanaSeleccionados,

        @Schema(description = "Hora de inicio general cuando mismoHorarioTodosLosDias=true", example = "08:00")
        @JsonAlias({"hora_inicio_general", "horaInicioPorDia"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaInicioGeneral,

        @Schema(description = "Hora de fin general cuando mismoHorarioTodosLosDias=true", example = "12:00")
        @JsonAlias({"hora_fin_general", "horaFinPorDia"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaFinGeneral,

        @Schema(description = "Horas de trabajo por día cuando mismoHorarioTodosLosDias=true", example = "4.0")
        @JsonAlias({"horas_trabajo_por_dia", "horasPorDia"})
        BigDecimal horasTrabajoPorDia,

        @Valid
        @Schema(description = "Cronograma detallado de días y horas a trabajar cuando unidad = DIAS")
        @JsonAlias({"dias", "diasTrabajo", "dias_trabajo", "horariosPorDia"})
        List<DiaTrabajoOfertaRequest> cronogramaDias
) {
    public static AproximacionDuracionRequest porDefecto() {
        return new AproximacionDuracionRequest(
                UnidadDuracionOferta.HORAS,
                BigDecimal.ONE,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    public AproximacionDuracionRequest(UnidadDuracionOferta unidad, BigDecimal cantidad) {
        this(unidad, cantidad, false, null, null, null, null, null, null, List.of());
    }

    public AproximacionDuracionRequest(
            UnidadDuracionOferta unidad,
            BigDecimal cantidad,
            Boolean mismoHorarioTodosLosDias,
            List<DiaTrabajoOfertaRequest> cronogramaDias
    ) {
        this(unidad, cantidad, mismoHorarioTodosLosDias, null, null, null, null, null, null, cronogramaDias);
    }
}


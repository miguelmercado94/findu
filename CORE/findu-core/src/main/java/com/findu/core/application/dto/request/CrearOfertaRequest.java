package com.findu.core.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "Solicitud para crear o contraproponer oferta")
public record CrearOfertaRequest(
        @NotNull @JsonAlias({"solicitudId", "solicitud_servicio_id"}) Long solicitudServicioId,
        @NotNull @JsonAlias({"proveedorId", "perfil_proveedor_id"}) Long perfilProveedorId,
        @JsonAlias({"montoOferta", "valor_propuesto", "precioPropuesto"}) BigDecimal valorPropuesto,
        @Schema(description = "Hora de inicio propuesta por el proveedor (formato HH:mm o HH:mm:ss)", example = "14:30:00")
        @JsonAlias({"horaInicioPropuesta", "hora_inicio_propuesta", "horaPropuesta", "horaInicio"})
        @JsonFormat(pattern = "HH:mm[:ss]")
        LocalTime horaInicioPropuesta,
        @JsonAlias({"tiempoEstimadoLlegada", "tiempo_estimado"}) String tiempoEstimado,
        @JsonAlias({"comentario", "mensaje_presentacion"}) String mensajePresentacion
) {
    public CrearOfertaRequest(Long solicitudServicioId, Long perfilProveedorId, BigDecimal valorPropuesto, String tiempoEstimado, String mensajePresentacion) {
        this(solicitudServicioId, perfilProveedorId, valorPropuesto, null, tiempoEstimado, mensajePresentacion);
    }
}


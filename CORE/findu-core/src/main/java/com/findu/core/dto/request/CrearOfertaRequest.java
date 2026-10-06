package com.findu.core.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearOfertaRequest(
        @NotNull @JsonAlias({"solicitudId", "solicitud_servicio_id"}) Long solicitudServicioId,
        @NotNull @JsonAlias({"proveedorId", "perfil_proveedor_id"}) Long perfilProveedorId,
        @NotNull @JsonAlias({"montoOferta", "valor_propuesto"}) BigDecimal valorPropuesto,
        @JsonAlias({"tiempoEstimadoLlegada", "tiempo_estimado"}) String tiempoEstimado,
        @JsonAlias({"comentario", "mensaje_presentacion"}) String mensajePresentacion
) {}


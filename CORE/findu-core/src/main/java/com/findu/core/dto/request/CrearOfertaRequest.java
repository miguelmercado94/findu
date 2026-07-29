package com.findu.core.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearOfertaRequest(
        @NotNull Long solicitudServicioId,
        @NotNull Long perfilProveedorId,
        @NotNull BigDecimal valorPropuesto,
        String tiempoEstimado,
        String mensajePresentacion
) {}

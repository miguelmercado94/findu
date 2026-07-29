package com.findu.core.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CrearSolicitudRequest(
        @NotNull Long perfilClienteId,
        @NotNull Long servicioId,
        @NotNull Long direccionId,
        LocalDateTime fechaProgramada,
        String nombreContacto,
        String telefonoContacto,
        Integer prioridad,
        BigDecimal presupuestoMaximo,
        Integer cantidadEstimada
) {}

package com.findu.core.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModificarSolicitudRequest(
        Long direccionId,
        LocalDateTime fechaProgramada,
        String nombreContacto,
        String telefonoContacto,
        Integer prioridad,
        BigDecimal presupuestoMaximo
) {}

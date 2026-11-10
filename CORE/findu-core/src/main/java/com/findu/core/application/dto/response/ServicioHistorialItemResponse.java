package com.findu.core.application.dto.response;

import java.time.LocalDateTime;

/** Item de historial: sin id de solicitud, sin codigo de ticket, sin direccion/telefono/detalles/presupuesto. */
public record ServicioHistorialItemResponse(String servicio, String categoria, LocalDateTime fecha, String estado) {
}
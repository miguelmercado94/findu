package com.findu.core.domain.model;

import java.time.LocalDateTime;

/** Fila de historial proveedor-cliente: sin id de solicitud, sin codigo, sin datos de contacto. */
public record ServicioHistorialItem(String servicio, String categoria, LocalDateTime fecha, String estado) {
}
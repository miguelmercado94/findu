package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.ChatEstado;

import java.util.UUID;

/** Whitelist de "Mis clientes" (detalle). Mismas claves que {@link ClienteResumenResponse}. */
public record ClienteDetalleResponse(
        UUID clienteRef,
        String nombreCompleto,
        boolean tieneFoto,
        boolean favoritoInterno,
        long serviciosRealizados,
        boolean enServicio,
        boolean activo,
        ChatEstado chatEstado) {
}
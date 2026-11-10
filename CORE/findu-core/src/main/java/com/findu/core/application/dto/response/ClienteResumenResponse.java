package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.ChatEstado;

import java.util.UUID;

/** Whitelist de "Mis clientes" (lista). Solo nombre y foto del cliente; ningun dato de contacto ni ticket. */
public record ClienteResumenResponse(
        UUID clienteRef,
        String nombreCompleto,
        boolean tieneFoto,
        boolean favoritoInterno,
        long serviciosRealizados,
        boolean enServicio,
        boolean activo,
        ChatEstado chatEstado) {
}
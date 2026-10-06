package com.findu.dispatcher.domain.model;

public record Interoperante(
        String usuarioId,
        String rol // CLIENTE, PROVEEDOR, GESTOR_AYUDA, SISTEMA
) {}

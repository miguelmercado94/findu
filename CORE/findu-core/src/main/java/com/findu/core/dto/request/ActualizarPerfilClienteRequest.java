package com.findu.core.dto.request;

public record ActualizarPerfilClienteRequest(
        String nombreCompleto,
        String urlImagenPerfil
) {}

package com.findu.core.application.dto.request;

public record ActualizarPerfilClienteRequest(
        String nombreCompleto,
        String urlImagenPerfil
) {}

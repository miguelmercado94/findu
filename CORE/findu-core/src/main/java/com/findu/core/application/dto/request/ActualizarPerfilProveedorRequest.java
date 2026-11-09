package com.findu.core.application.dto.request;

import java.time.LocalDate;

public record ActualizarPerfilProveedorRequest(
        String nombreCompleto,
        String urlImagenPerfil,
        LocalDate fechaNacimiento
) {}

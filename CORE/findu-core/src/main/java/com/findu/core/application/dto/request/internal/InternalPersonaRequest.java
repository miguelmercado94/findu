package com.findu.core.application.dto.request.internal;

public record InternalPersonaRequest(
        String nombreCompleto,
        String tipoDocumento,
        String numeroDocumento,
        String fechaNacimiento,
        String sexo
) {
}

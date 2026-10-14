package com.findu.core.application.dto.request.internal;

public record InternalDireccionRequest(
        Long municipioId,
        String direccionTexto,
        Double lat,
        Double lng
) {
}

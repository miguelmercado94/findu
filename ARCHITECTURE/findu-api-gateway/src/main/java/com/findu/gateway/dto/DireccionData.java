package com.findu.gateway.dto;

import jakarta.validation.constraints.NotNull;

public record DireccionData(
        @NotNull Long municipioId,
        String direccionTexto,
        Double lat,
        Double lng
) {
}

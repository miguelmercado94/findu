package com.findu.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CrearPortafolioRequest(
        @NotBlank String titulo,
        String descripcion,
        String urlImagen
) {}

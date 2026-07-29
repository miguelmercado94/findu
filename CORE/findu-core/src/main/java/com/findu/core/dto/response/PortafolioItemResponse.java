package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de item de portafolio")
public record PortafolioItemResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Título") String titulo,
        @Schema(description = "Descripción") String descripcion,
        @Schema(description = "URL de la imagen") String urlImagen
) {}

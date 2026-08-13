package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de servicio")
public record ServicioResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre") String nombre,
        @Schema(description = "Descripción") String descripcion,
        @Schema(description = "Tipo de cobro") String tipoCobro,
        @Schema(description = "URL de la imagen de presentación (S3)") String urlImagen
) {}

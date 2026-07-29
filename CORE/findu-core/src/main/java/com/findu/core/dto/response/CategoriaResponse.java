package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Respuesta de categoría")
public record CategoriaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre") String nombre,
        @Schema(description = "Descripción") String descripcion,
        @Schema(description = "Subcategorías") List<CategoriaResponse> subcategorias
) {}

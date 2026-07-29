package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de municipio")
public record MunicipioResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Código DANE") String codigoDane,
        @Schema(description = "Nombre") String nombre,
        @Schema(description = "Departamento") String departamento
) {}

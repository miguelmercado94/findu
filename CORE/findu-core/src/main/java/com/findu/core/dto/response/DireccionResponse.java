package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Respuesta de dirección")
public record DireccionResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Etiqueta") String etiqueta,
        @Schema(description = "Texto de la dirección") String direccionTexto,
        @Schema(description = "Nombre del municipio") String municipioNombre,
        @Schema(description = "Latitud") BigDecimal latitud,
        @Schema(description = "Longitud") BigDecimal longitud,
        @Schema(description = "Piso") String piso,
        @Schema(description = "Apartamento") String apartamento,
        @Schema(description = "Referencia") String referencia,
        @Schema(description = "Es principal") boolean esPrincipal
) {}

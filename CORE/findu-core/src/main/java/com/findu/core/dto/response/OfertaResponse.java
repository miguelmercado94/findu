package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Respuesta de oferta")
public record OfertaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre del proveedor") String proveedorNombre,
        @Schema(description = "Calificación del proveedor") BigDecimal calificacionProveedor,
        @Schema(description = "Valor propuesto") BigDecimal valorPropuesto,
        @Schema(description = "Tiempo estimado") String tiempoEstimado,
        @Schema(description = "Mensaje de presentación") String mensajePresentacion,
        @Schema(description = "Estado de la oferta") String estadoOferta
) {}

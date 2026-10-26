package com.findu.core.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "Respuesta de oferta")
public record OfertaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "ID del perfil de proveedor") Long perfilProveedorId,
        @Schema(description = "Nombre del proveedor") String proveedorNombre,
        @Schema(description = "Foto de perfil del proveedor") String proveedorFoto,
        @Schema(description = "Calificación del proveedor") BigDecimal calificacionProveedor,
        @Schema(description = "Valor propuesto") BigDecimal valorPropuesto,
        @Schema(description = "Hora de inicio propuesta por el proveedor", example = "14:30:00")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime horaInicioPropuesta,
        @Schema(description = "Tiempo estimado") String tiempoEstimado,
        @Schema(description = "Mensaje de presentación") String mensajePresentacion,
        @Schema(description = "Estado de la oferta") String estadoOferta
) {
    public OfertaResponse(Long id, Long perfilProveedorId, String proveedorNombre, String proveedorFoto,
                          BigDecimal calificacionProveedor, BigDecimal valorPropuesto,
                          String tiempoEstimado, String mensajePresentacion, String estadoOferta) {
        this(id, perfilProveedorId, proveedorNombre, proveedorFoto, calificacionProveedor, valorPropuesto, null, tiempoEstimado, mensajePresentacion, estadoOferta);
    }
}

package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle de una especialidad del proveedor (con nombre del servicio).
 */
@Schema(description = "Detalle de especialidad del proveedor")
public record PerfilEspecialistaDetalleResponse(
        @Schema(description = "Nombre del servicio", example = "Enfermería a Domicilio")
        String servicioNombre,

        @Schema(description = "Descripción de experiencia", example = "5 años de experiencia en cuidado domiciliario")
        String descripcion,

        @Schema(description = "Años de experiencia", example = "5")
        Integer experienciaAnios
) {}

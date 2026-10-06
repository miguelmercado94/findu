package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Respuesta de perfil especialista")
public record PerfilEspecialistaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "ID del servicio del catálogo") Long servicioId,
        @Schema(description = "Nombre del servicio") String servicioNombre,
        @Schema(description = "Descripción") String descripcion,
        @Schema(description = "Años de experiencia") Integer experienciaAnios,
        @Schema(description = "Calificación promedio del perfil especialista") BigDecimal calificacionPromedio,
        @Schema(description = "Activo") boolean active
) {}

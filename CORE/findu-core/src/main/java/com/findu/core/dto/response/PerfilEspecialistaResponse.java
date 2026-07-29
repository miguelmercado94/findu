package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de perfil especialista")
public record PerfilEspecialistaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre del servicio") String servicioNombre,
        @Schema(description = "Descripción") String descripcion,
        @Schema(description = "Años de experiencia") Integer experienciaAnios,
        @Schema(description = "Activo") boolean active
) {}

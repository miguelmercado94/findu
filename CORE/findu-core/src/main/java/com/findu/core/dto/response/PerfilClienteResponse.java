package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Respuesta resumida del perfil de cliente")
public record PerfilClienteResponse(
        @Schema(description = "ID del perfil") Long id,
        @Schema(description = "Nombre completo") String nombreCompleto,
        @Schema(description = "Celular") String celular,
        @Schema(description = "Código telefónico internacional") String codPhoneInternational,
        @Schema(description = "Sexo") String sexo,
        @Schema(description = "URL imagen de perfil") String urlImagenPerfil,
        @Schema(description = "Calificación promedio") BigDecimal calificacionPromedio,
        @Schema(description = "Estado del perfil") String estado
) {}

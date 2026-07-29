package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Respuesta detallada del perfil de proveedor")
public record PerfilProveedorDetalleResponse(
        @Schema(description = "ID del perfil") Long id,
        @Schema(description = "Nombre completo") String nombreCompleto,
        @Schema(description = "Número de identificación") String numeroIdentificacion,
        @Schema(description = "Tipo de identificación") String tipoIdentificacion,
        @Schema(description = "Fecha de nacimiento") LocalDate fechaNacimiento,
        @Schema(description = "Sexo") String sexo,
        @Schema(description = "Celular") String celular,
        @Schema(description = "Código telefónico internacional") String codPhoneInternational,
        @Schema(description = "URL imagen de perfil") String urlImagenPerfil,
        @Schema(description = "Calificación promedio") BigDecimal calificacionPromedio,
        @Schema(description = "Estado del perfil") String estado,
        @Schema(description = "Estado de verificación") String estadoVerificacion,
        @Schema(description = "Especialidades") List<PerfilEspecialistaResponse> especialidades,
        @Schema(description = "Direcciones") List<DireccionResponse> direcciones
) {}

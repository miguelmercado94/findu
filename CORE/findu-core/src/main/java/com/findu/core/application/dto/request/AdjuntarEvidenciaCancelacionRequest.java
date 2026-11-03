package com.findu.core.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Carga posterior de evidencias para un ticket de cancelación por fuerza mayor")
public record AdjuntarEvidenciaCancelacionRequest(
        @Schema(description = "ID del proveedor", example = "10")
        Long perfilProveedorId,

        @NotEmpty(message = "Debe enviar al menos una evidencia o soporte")
        @Schema(description = "Lista de URLs o referencias de archivos/imágenes de soporte")
        List<String> evidenciasUrls,

        @Schema(description = "Comentario adicional sobre el soporte adjuntado")
        String comentario
) {}


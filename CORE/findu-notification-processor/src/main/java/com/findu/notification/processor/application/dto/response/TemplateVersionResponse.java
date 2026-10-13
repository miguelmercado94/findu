package com.findu.notification.processor.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada del histórico de versiones de una plantilla (diseño §9). Resume cada versión
 * inmutable para listados de histórico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen de una versión inmutable de una plantilla")
public class TemplateVersionResponse {

    @Schema(description = "Número de versión", example = "2")
    private Integer version;

    @Schema(description = "Si esta versión está activa", example = "true")
    private boolean active;

    @Schema(description = "Fecha de creación (ISO-8601)")
    private String createdAt;

    @Schema(description = "Autor de la versión")
    private String createdBy;

    @Schema(description = "Cuerpo de la plantilla en esa versión")
    private String bodyTemplate;
}

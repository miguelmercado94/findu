package com.findu.notification.processor.application.dto.request;

import com.findu.notification.processor.domain.enums.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Petición para activar (o hacer rollback a) una versión previa de una plantilla,
 * repuntando el {@code v#ACTIVE} (diseño §9).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Activa o hace rollback a una versión concreta de la plantilla")
public class ActivateVersionRequest {

    @NotBlank
    @Schema(description = "Código funcional de la plantilla", example = "VERIFICACION_OTP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    @NotNull
    @Schema(description = "Tipo funcional de la plantilla", example = "SMS", requiredMode = Schema.RequiredMode.REQUIRED)
    private TemplateType tipo;

    @NotBlank
    @Schema(description = "Idioma de la plantilla", example = "es", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotNull
    @Schema(description = "Número de versión a activar", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}

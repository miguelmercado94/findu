package com.findu.validacion.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alerta activa para el usuario")
public record ActiveAlertDto(
        @Schema(description = "UUID único de la instancia de alerta", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String alertUuid,
        @Schema(description = "Código funcional de la alerta (alertTemplateCode o validationCode)", example = "PERFIL_CORREO_PENDIENTE")
        String codigo,
        @Schema(description = "Código de validación", example = "EMAIL_VERIFICATION")
        String validationCode,
        @Schema(description = "Código de plantilla de alerta", example = "PERFIL_CORREO_PENDIENTE")
        String alertTemplateCode,
        @Schema(description = "Indica si la alerta está activa", example = "true")
        boolean active,
        @Schema(description = "Categoría de la alerta", example = "INFO")
        String categoria,
        @Schema(description = "Título de la alerta", example = "Confirma tu correo")
        String titulo,
        @Schema(description = "Cuerpo descriptivo de la alerta")
        String body,
        @Schema(description = "Etiqueta del CTA", example = "Validar ahora")
        String ctaLabel,
        @Schema(description = "Acción / deep link del CTA", example = "findu://perfil/verificacion-biometrica")
        String ctaAction,
        @Schema(description = "Si la alerta es cerrable por el usuario", example = "true")
        boolean cerrable,
        @Schema(description = "Prioridad de visualización", example = "1")
        Integer prioridad,
        @Schema(description = "Pantalla destino de la alerta", example = "home")
        String pantallaDestino
) {
}

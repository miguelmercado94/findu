package com.findu.core.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Resolución de soporte sobre una solicitud o ticket de cancelación")
public record ResolverCancelacionSoporteRequest(
        @Schema(description = "ID del agente de soporte responsable", example = "1")
        Long agenteSoporteId,

        @NotNull(message = "Debe indicar si aprueba/valida el caso (true) o lo rechaza/declara injustificado (false)")
        @Schema(description = "true = Aprueba cancelación (Cat 1) / Valida evidencia (Cat 2) / Declara justificada (Cat 4); false = Rechaza / Declara injustificada")
        Boolean aprobar,

        @Schema(description = "Justificación o detalle de la decisión de soporte")
        String justificacion
) {}


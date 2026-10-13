package com.findu.notification.processor.application.dto.response;

import com.findu.notification.processor.domain.enums.AlertCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proyección de una plantilla tipo ALERTA lista para que el cliente la pinte en una
 * pantalla (diseño §7.4). Combina el {@code body_template} de nivel superior con los
 * campos de UI que viven bajo {@code alert_meta}. El cliente evalúa
 * {@code condicionAparicion} contra el estado del perfil y ordena por {@code prioridad}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Alerta de UI resuelta para una pantalla (contenido editable por admin)")
public class ScreenAlertResponse {

    @Schema(description = "Código funcional de la plantilla de alerta", example = "PERFIL_CORREO_PENDIENTE")
    private String codigo;

    @Schema(description = "Categoría de la alerta", example = "INFO")
    private AlertCategory categoria;

    @Schema(description = "Título de la alerta", example = "Confirma tu correo")
    private String titulo;

    @Schema(description = "Cuerpo/descripción de la alerta")
    private String body;

    @Schema(description = "Etiqueta del call-to-action (opcional)", example = "Validar ahora")
    private String ctaLabel;

    @Schema(description = "Acción/deep link del call-to-action (opcional)", example = "findu://perfil/verificacion-biometrica")
    private String ctaAction;

    @Schema(description = "Prioridad de orden (ascendente)", example = "1")
    private Integer prioridad;

    @Schema(description = "Expresión evaluada por el cliente para mostrar/ocultar la alerta", example = "emailOk == false")
    private String condicionAparicion;

    @Schema(description = "Segundos para auto-ocultar; 0 = sin temporizador (gobernada por la condición)", example = "0")
    private Integer tiempoDesaparicion;
}

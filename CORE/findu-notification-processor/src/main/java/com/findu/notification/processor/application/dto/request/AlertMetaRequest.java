package com.findu.notification.processor.application.dto.request;

import com.findu.notification.processor.domain.enums.AlertCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata de UI para una plantilla tipo ALERTA (diseño §3.2 / §5).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Metadata de presentación de una ALERTA de UI")
public class AlertMetaRequest {

    @Schema(description = "Categoría de la alerta", example = "SEGURIDAD")
    private AlertCategory categoria;

    @Schema(description = "Pantalla destino donde se muestra la alerta", example = "PERFIL")
    private String pantallaDestino;

    @Schema(description = "Título de la alerta", example = "Completa tu activación")
    private String titulo;

    @Schema(description = "Etiqueta del botón de acción", example = "Verificar correo")
    private String ctaLabel;

    @Schema(description = "Acción asociada al botón", example = "OPEN_EMAIL_VERIFICATION")
    private String ctaAction;

    @Schema(description = "Si la alerta puede cerrarse por el usuario", example = "true")
    private boolean cerrable;

    @Schema(description = "Prioridad de presentación (menor = mayor prioridad)", example = "1")
    private Integer prioridad;

    @Schema(description = "Inicio de vigencia (ISO-8601)", example = "2025-01-01T00:00:00Z")
    private String vigenciaDesde;

    @Schema(description = "Fin de vigencia (ISO-8601)", example = "2025-12-31T23:59:59Z")
    private String vigenciaHasta;

    @Schema(description = "Condición de aparición de la alerta", example = "emailNoVerificado")
    private String condicionAparicion;

    @Schema(description = "Segundos tras los que la alerta desaparece (0/null = persistente)", example = "10")
    private Integer tiempoDesaparicion;
}

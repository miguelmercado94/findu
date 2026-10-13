package com.findu.notification.processor.application.dto.response;

import com.findu.notification.processor.domain.enums.AlertCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vista de salida de la metadata de UI de una plantilla tipo ALERTA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Metadata de presentación de una ALERTA de UI")
public class AlertMetaResponse {
    private AlertCategory categoria;
    private String pantallaDestino;
    private String titulo;
    private String ctaLabel;
    private String ctaAction;
    private boolean cerrable;
    private Integer prioridad;
    private String vigenciaDesde;
    private String vigenciaHasta;
    private String condicionAparicion;
    private Integer tiempoDesaparicion;
}

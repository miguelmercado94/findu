package com.findu.notification.processor.domain.model;

import com.findu.notification.processor.domain.enums.AlertCategory;
import lombok.*;

/**
 * Metadata de UI para una plantilla tipo ALERTA (diseño §3.2).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertMeta {
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

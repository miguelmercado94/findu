package com.findu.notification.processor.application.dto.request;

import com.findu.notification.processor.domain.enums.AudienceMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Audiencia de una plantilla (diseño §4). El payload depende del {@code modo}:
 * TODOS (sin payload), SEGMENTO ({@code filtros}) o USUARIO ({@code userIds}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audiencia objetivo de la plantilla")
public class AudienceRequest {

    @Schema(description = "Modo de audiencia", example = "TODOS")
    private AudienceMode modo;

    @Schema(description = "Filtros de segmentación (sólo modo SEGMENTO)")
    private Map<String, Object> filtros;

    @Schema(description = "IDs de usuario destino (sólo modo USUARIO)")
    private List<String> userIds;
}

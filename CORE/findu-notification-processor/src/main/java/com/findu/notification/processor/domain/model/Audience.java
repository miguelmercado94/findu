package com.findu.notification.processor.domain.model;

import com.findu.notification.processor.domain.enums.AudienceMode;
import lombok.*;

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
public class Audience {
    private AudienceMode modo;
    private Map<String, Object> filtros;
    private List<String> userIds;
}

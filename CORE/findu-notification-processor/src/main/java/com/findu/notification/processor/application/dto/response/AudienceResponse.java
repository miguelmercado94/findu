package com.findu.notification.processor.application.dto.response;

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
 * Vista de salida de la audiencia de una plantilla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Audiencia objetivo de la plantilla")
public class AudienceResponse {
    private AudienceMode modo;
    private Map<String, Object> filtros;
    private List<String> userIds;
}

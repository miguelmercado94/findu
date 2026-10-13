package com.findu.notification.processor.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vista de salida del enrutamiento de una plantilla tipo NOTIFICACION.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Enrutamiento condicional por presencia del usuario (tipo NOTIFICACION)")
public class NotificationRoutingResponse {
    private String condicion;
    private String canalSiActivo;
    private String canalSiInactivo;
}

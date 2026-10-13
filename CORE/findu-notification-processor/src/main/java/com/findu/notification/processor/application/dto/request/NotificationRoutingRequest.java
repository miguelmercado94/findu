package com.findu.notification.processor.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enrutamiento de una plantilla tipo NOTIFICACION (diseño §3.2): si el usuario
 * está activo va por {@code canalSiActivo} (WEBSOCKET), si no por
 * {@code canalSiInactivo} (PUSH).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Enrutamiento condicional por presencia del usuario (tipo NOTIFICACION)")
public class NotificationRoutingRequest {

    @Schema(description = "Condición que determina el canal", example = "isUserActivo")
    private String condicion;

    @Schema(description = "Canal cuando el usuario está activo", example = "WEBSOCKET")
    private String canalSiActivo;

    @Schema(description = "Canal cuando el usuario está inactivo", example = "PUSH")
    private String canalSiInactivo;
}

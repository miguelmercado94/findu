package com.findu.notification.processor.domain.model;

import lombok.*;

/**
 * Enrutamiento de una plantilla tipo NOTIFICACION (diseño §3.2): si el usuario
 * está activo va por {@code canalSiActivo} (WEBSOCKET), si no por
 * {@code canalSiInactivo} (PUSH). El dispatcher es la fuente de verdad de presencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRouting {
    private String condicion;
    private String canalSiActivo;
    private String canalSiInactivo;
}

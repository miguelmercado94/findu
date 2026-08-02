package com.findu.notification.model;

import lombok.*;

import java.util.Map;

/**
 * Registro de notificación para persistencia.
 * Independiente de la tecnología de almacenamiento (DynamoDB, MongoDB, etc.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecord {

    /** ID único generado por la Lambda */
    private String notificationId;

    /** Tipo: EMAIL, SMS, PUSH */
    private String type;

    /** Destinatario */
    private String recipient;

    /** Código de plantilla */
    private String templateCode;

    /** Idioma */
    private String language;

    /** Parámetros de la plantilla */
    private Map<String, String> params;

    /** Para PUSH: true = solo enviar si el usuario está conectado (WebSocket/SSE), false = enviar siempre via FCM */
    private boolean requiresConnection;

    /** Si se publicó exitosamente al bus de eventos */
    private boolean dispatched;

    /** Error si falló el despacho */
    private String error;

    /** Timestamp de creación (ISO 8601) */
    private String createdAt;
}

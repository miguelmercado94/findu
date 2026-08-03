package com.findu.notification.model;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Registro de notificación para persistencia y para el bus de eventos.
 * Independiente de la tecnología de almacenamiento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecord {

    /** ID único generado por la Lambda */
    private String notificationId;

    /** Tipo: EMAIL, SMS, PUSH, WHATSAPP */
    private String type;

    /** Destinatario */
    private String recipient;

    /** Código de plantilla */
    private String templateCode;

    /** Idioma */
    private String language;

    /** Parámetros de la plantilla (key-value plano) */
    private Map<String, String> params;

    /** Para PUSH: true = solo enviar si el usuario está conectado */
    private boolean requiresConnection;

    /** Anexos (solo para EMAIL) — URL S3 + nombre + tipo MIME */
    private List<Attachment> attachments;

    /** Si se publicó exitosamente al bus de eventos */
    private boolean dispatched;

    /** Error si falló el despacho */
    private String error;

    /** Timestamp de creación (ISO 8601) */
    private String createdAt;
}

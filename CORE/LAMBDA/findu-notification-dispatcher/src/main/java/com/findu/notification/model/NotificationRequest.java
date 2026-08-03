package com.findu.notification.model;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Body que recibe la Lambda (POST).
 * El notification_id NO viene en el request, lo genera la Lambda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /** Tipo: EMAIL, SMS, PUSH, WHATSAPP */
    private String type;

    /** Destinatario: email, teléfono o username según el tipo */
    private String recipient;

    /** Código de plantilla (ej: ORDER_CONFIRMATION, OFERTA_RECIBIDA) */
    private String templateCode;

    /** Idioma de la plantilla (es, en) */
    @Builder.Default
    private String language = "es";

    /** Datos dinámicos para la plantilla (solo key-value plano, sin HTML ni JSON) */
    private Map<String, String> params;

    /** Si es PUSH: true = enviar solo cuando el usuario esté conectado (real-time), false = enviar siempre al dispositivo */
    private boolean requiresConnection;

    /** Anexos (solo aplica para EMAIL). URL S3 + nombre + tipo MIME */
    private List<Attachment> attachments;
}

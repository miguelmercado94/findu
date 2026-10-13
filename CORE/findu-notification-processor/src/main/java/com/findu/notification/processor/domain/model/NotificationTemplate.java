package com.findu.notification.processor.domain.model;

import com.findu.notification.processor.domain.enums.TemplateType;
import lombok.*;

import java.util.List;

/**
 * Modelo de dominio para plantillas de notificación (diseño §3).
 * Persistidas en DynamoDB (tabla: notification_templates, PK id + SK sk versionada).
 *
 * Agrupa los campos comunes a todos los tipos y los específicos por tipo
 * (EMAIL / WHATSAPP / EVENTO / NOTIFICACION / ALERTA). Los value objects
 * {@link Audience}, {@link NotificationRouting} y {@link AlertMeta} evitan mapas crudos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    // ---- Campos comunes (diseño §3.1) ----
    private String id;            // PK — "CODE#TIPO#LANG" (ej: "VERIFICACION_OTP#SMS#es")
    private String sk;            // SK de versión — "v#<n>" / "v#ACTIVE"
    private String codigo;        // Código funcional (ej: VERIFICACION_OTP)
    private TemplateType tipo;    // Tipo funcional de la plantilla
    private String language;      // es, en
    private String bodyTemplate;  // Cuerpo con placeholders {{var}}
    private List<String> variables;
    private Integer version;
    private boolean active;
    private Audience audiencia;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

    // ---- Legacy / canal técnico (se conserva mientras los senders usen NotificationChannel) ----
    private String channel;       // EMAIL, SMS, PUSH, WHATSAPP (derivable de 'tipo')

    // ---- EMAIL ----
    private String subject;
    private String contentType;   // TXT | HTML (compartido con WHATSAPP)

    // ---- WHATSAPP ----
    private String waTemplateName;

    // ---- EVENTO ----
    private String rabbitExchange;
    private String rabbitRoutingKey;
    private String rabbitQueue;
    private String eventType;

    // ---- NOTIFICACION ----
    private String title;
    private NotificationRouting routing;

    // ---- ALERTA ----
    private AlertMeta alertMeta;
}

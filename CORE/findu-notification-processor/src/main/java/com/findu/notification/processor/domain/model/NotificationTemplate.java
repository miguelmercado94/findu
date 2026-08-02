package com.findu.notification.processor.domain.model;

import lombok.*;

/**
 * Modelo de dominio para plantillas de notificación.
 * Persistidas en DynamoDB (tabla: notification_templates).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    private String id;           // PK — ej: "ORDER_DELIVERED#EMAIL#es"
    private String channel;      // EMAIL, SMS, PUSH, WHATSAPP
    private String language;     // es, en
    private String contentType;  // TXT o HTML — define cómo se renderiza el body
    private String subject;      // Solo para EMAIL (asunto)
    private String bodyTemplate; // "Hola {{user_name}}, tu pedido #{{order_id}}..."
    private Integer version;
    private boolean active;
}

package com.findu.notification.processor.application.dto.response;

import com.findu.notification.processor.domain.enums.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Vista completa de la versión activa de una plantilla (diseño §10). Expone los
 * campos comunes, de versionado/auditoría y los específicos por tipo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vista de la versión activa de una plantilla")
public class TemplateResponse {

    // ---- Comunes ----
    private String id;
    private String codigo;
    private TemplateType tipo;
    private String language;
    private String bodyTemplate;
    private List<String> variables;
    private Integer version;
    private boolean active;
    private AudienceResponse audiencia;

    // ---- Auditoría ----
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

    // ---- EMAIL / WHATSAPP ----
    private String subject;
    private String contentType;
    private String waTemplateName;

    // ---- EVENTO ----
    private String rabbitExchange;
    private String rabbitRoutingKey;
    private String rabbitQueue;
    private String eventType;

    // ---- NOTIFICACION ----
    private String title;
    private NotificationRoutingResponse routing;

    // ---- ALERTA ----
    private AlertMetaResponse alertMeta;
}

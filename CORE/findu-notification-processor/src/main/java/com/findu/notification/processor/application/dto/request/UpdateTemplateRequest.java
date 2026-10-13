package com.findu.notification.processor.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Petición para editar una plantilla (diseño §10). El código, tipo e idioma identifican
 * la plantilla y viajan en la ruta; aquí van los campos editables. La edición genera
 * una <b>versión nueva</b> inmutable (no sobreescribe la versión anterior).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Campos editables de una plantilla (genera una versión nueva)")
public class UpdateTemplateRequest {

    @NotBlank
    @Schema(description = "Cuerpo con placeholders {{var}}", example = "Tu código es {{codigo}}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bodyTemplate;

    @Schema(description = "Variables declaradas por la plantilla")
    private List<String> variables;

    @Valid
    @Schema(description = "Audiencia objetivo de la plantilla")
    private AudienceRequest audiencia;

    // ---- EMAIL / WHATSAPP ----
    @Schema(description = "Asunto (tipo EMAIL)")
    private String subject;

    @Schema(description = "Tipo de contenido (EMAIL/WHATSAPP): TXT | HTML")
    private String contentType;

    @Schema(description = "Nombre de la plantilla aprobada de WhatsApp (tipo WHATSAPP)")
    private String waTemplateName;

    // ---- EVENTO ----
    @Schema(description = "Exchange de RabbitMQ destino (tipo EVENTO)")
    private String rabbitExchange;

    @Schema(description = "Routing key de RabbitMQ (tipo EVENTO)")
    private String rabbitRoutingKey;

    @Schema(description = "Cola de RabbitMQ destino (tipo EVENTO)")
    private String rabbitQueue;

    @Schema(description = "Tipo de evento (tipo EVENTO)")
    private String eventType;

    // ---- NOTIFICACION ----
    @Schema(description = "Título (tipo NOTIFICACION)")
    private String title;

    @Valid
    @Schema(description = "Enrutamiento por presencia (tipo NOTIFICACION)")
    private NotificationRoutingRequest routing;

    // ---- ALERTA ----
    @Valid
    @Schema(description = "Metadata de UI (tipo ALERTA)")
    private AlertMetaRequest alertMeta;
}

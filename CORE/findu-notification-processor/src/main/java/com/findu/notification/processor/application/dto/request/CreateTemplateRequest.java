package com.findu.notification.processor.application.dto.request;

import com.findu.notification.processor.domain.enums.TemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Petición para crear una plantilla nueva (diseño §10, superficie admin). Agrupa los
 * campos comunes y los específicos por tipo (sólo se usan los que apliquen al {@code tipo}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para crear una plantilla de notificación")
public class CreateTemplateRequest {

    // ---- Comunes ----
    @NotBlank
    @Schema(description = "Código funcional de la plantilla", example = "VERIFICACION_OTP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String codigo;

    @NotNull
    @Schema(description = "Tipo funcional de la plantilla", example = "SMS", requiredMode = Schema.RequiredMode.REQUIRED)
    private TemplateType tipo;

    @NotBlank
    @Schema(description = "Idioma de la plantilla", example = "es", requiredMode = Schema.RequiredMode.REQUIRED)
    private String language;

    @NotBlank
    @Schema(description = "Cuerpo con placeholders {{var}}", example = "Tu código es {{codigo}}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bodyTemplate;

    @Schema(description = "Variables declaradas por la plantilla")
    private List<String> variables;

    @Valid
    @Schema(description = "Audiencia objetivo de la plantilla")
    private AudienceRequest audiencia;

    // ---- EMAIL / WHATSAPP ----
    @Schema(description = "Asunto (tipo EMAIL)", example = "Verifica tu cuenta")
    private String subject;

    @Schema(description = "Tipo de contenido (EMAIL/WHATSAPP): TXT | HTML", example = "HTML")
    private String contentType;

    @Schema(description = "Nombre de la plantilla aprobada de WhatsApp (tipo WHATSAPP)", example = "otp_verificacion")
    private String waTemplateName;

    // ---- EVENTO ----
    @Schema(description = "Exchange de RabbitMQ destino (tipo EVENTO)", example = "notifications.exchange")
    private String rabbitExchange;

    @Schema(description = "Routing key de RabbitMQ (tipo EVENTO)", example = "notification.email")
    private String rabbitRoutingKey;

    @Schema(description = "Cola de RabbitMQ destino (tipo EVENTO)", example = "notification.email.queue")
    private String rabbitQueue;

    @Schema(description = "Tipo de evento (tipo EVENTO)", example = "PAYMENT_COMPLETED")
    private String eventType;

    // ---- NOTIFICACION ----
    @Schema(description = "Título (tipo NOTIFICACION)", example = "Pago recibido")
    private String title;

    @Valid
    @Schema(description = "Enrutamiento por presencia (tipo NOTIFICACION)")
    private NotificationRoutingRequest routing;

    // ---- ALERTA ----
    @Valid
    @Schema(description = "Metadata de UI (tipo ALERTA)")
    private AlertMetaRequest alertMeta;
}

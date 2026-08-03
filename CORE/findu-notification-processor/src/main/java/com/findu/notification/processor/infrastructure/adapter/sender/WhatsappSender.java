package com.findu.notification.processor.infrastructure.adapter.sender;

import com.findu.notification.processor.application.port.output.NotificationSenderPort;
import com.findu.notification.processor.domain.model.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Envío de mensajes via WhatsApp Business Cloud API (Meta).
 * Usa WebClient para llamar a la Graph API de Meta.
 *
 * API Docs: https://developers.facebook.com/docs/whatsapp/cloud-api/messages
 *
 * Requisitos:
 * - Cuenta de WhatsApp Business verificada
 * - WHATSAPP_PHONE_NUMBER_ID: ID del número de teléfono de negocio
 * - WHATSAPP_ACCESS_TOKEN: Token de acceso permanente de Meta
 *
 * El recipient debe ser el número del destinatario con código de país sin '+' (ej: 573003763300)
 */
@Component
public class WhatsappSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsappSender.class);
    private static final String META_GRAPH_API = "https://graph.facebook.com/v21.0";

    private final WebClient webClient;
    private final String phoneNumberId;
    private final boolean enabled;

    public WhatsappSender(
            @Value("${findu.notification.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${findu.notification.whatsapp.access-token:}") String accessToken,
            @Value("${findu.notification.whatsapp.enabled:false}") boolean enabled) {
        this.phoneNumberId = phoneNumberId;
        this.enabled = enabled;

        this.webClient = WebClient.builder()
                .baseUrl(META_GRAPH_API)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public NotificationResult send(String recipient, String subject, String body, Map<String, String> metadata) {
        if (!enabled || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("[WHATSAPP] Canal deshabilitado o sin configurar. Mensaje no enviado a={}", recipient);
            return NotificationResult.builder()
                    .success(false)
                    .channel(NotificationChannel.WHATSAPP)
                    .recipient(recipient)
                    .error("Canal WHATSAPP deshabilitado. Configure WHATSAPP_PHONE_NUMBER_ID y WHATSAPP_ACCESS_TOKEN.")
                    .build();
        }

        try {
            // Construir payload para la API de WhatsApp
            // Usamos mensaje de texto libre (no template pre-aprobado de Meta)
            // Para producción se recomienda usar templates aprobados para iniciar conversaciones
            Map<String, Object> payload = buildTextPayload(recipient, body);

            String response = webClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("[WHATSAPP] Mensaje enviado a={} response={}", recipient, response);

            return NotificationResult.builder()
                    .success(true)
                    .channel(NotificationChannel.WHATSAPP)
                    .recipient(recipient)
                    .build();

        } catch (Exception e) {
            log.error("[WHATSAPP] Error enviando mensaje a={}: {}", recipient, e.getMessage(), e);
            return NotificationResult.builder()
                    .success(false)
                    .channel(NotificationChannel.WHATSAPP)
                    .recipient(recipient)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Construye el payload para enviar un mensaje de texto libre.
     * Formato: https://developers.facebook.com/docs/whatsapp/cloud-api/messages/text-messages
     */
    private Map<String, Object> buildTextPayload(String recipient, String body) {
        return Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", sanitizePhone(recipient),
                "type", "text",
                "text", Map.of(
                        "preview_url", false,
                        "body", body
                )
        );
    }

    /**
     * Limpia el número de teléfono: quita '+' y espacios.
     * La API de Meta espera: código de país + número sin separadores (ej: 573003763300)
     */
    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
}

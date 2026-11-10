package com.findu.notification.processor.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.findu.notification.processor.domain.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Convierte el envelope de la cola fallback (usuario sin socket) en un {@link NotificationMessage} PUSH.
 *
 * <p>Logica pura, nunca lanza. Reglas (en orden):</p>
 * <ol>
 *   <li>{@code targetUserId} en blanco -> vacio (WARN).</li>
 *   <li>{@code CHAT_MESSAGE} o payload con {@code content} -> rama TICKET_CHAT identica a la historica.</li>
 *   <li>{@code targetUserId} que no empieza por {@code PROVEEDOR_} -> vacio (DEBUG): solo los proveedores
 *       tienen device tokens (ver {@code DeviceTokenService.resolveCanonicalRecipient}).</li>
 *   <li>{@code OFFER_RECEIVED} -> push generico. OJO: core lo emite al CLIENTE
 *       ({@code DispatcherNotificationObserver.onOfertaRecibida}), por lo que hoy esta rama solo se
 *       mantiene por compatibilidad; el flujo real de oferta del proveedor es {@code SERVICE_UPDATE}
 *       ({@code OFERTA_ACEPTADA}, {@code NUEVA_SOLICITUD}, ...).</li>
 *   <li>{@code SERVICE_UPDATE} -> push si su {@code tipoEvento} esta en la lista blanca.</li>
 *   <li>{@code SYSTEM_ALERT} -> push solo con {@code findu.push.alerts-enabled=true}.</li>
 * </ol>
 *
 * <p>"NUEVA_SOLICITUD solo a proveedores elegibles en linea": core calcula la lista en
 * {@code GestionSolicitudesUseCaseImpl} con {@code proveedorService.findEligibleProviders(servicioId, municipioId)}
 * (query de {@code PerfilProveedorRepository}: {@code disponible = true AND estado = 'ACTIVO'}, con la
 * especialidad activa y la cobertura del municipio) y luego {@code filtrarProveedoresSinRestriccion};
 * {@code DispatcherNotificationObserver.onSolicitudCreada} notifica solo a esa lista. "En linea" es el
 * flag {@code disponible} (toggle), no presencia WebSocket.</p>
 */
@Component
public class FallbackPushMapper {

    private static final Logger log = LoggerFactory.getLogger(FallbackPushMapper.class);

    static final String DEFAULT_SERVICE_UPDATE_TYPES =
            "NUEVA_SOLICITUD,SOLICITUD_ACTUALIZADA,OFERTA_ACEPTADA,ALARMA_SERVICIO_1H,AMONESTACION_GENERADA,"
                    + "RESTRICCION_ACTIVADA,RESOLUCION_CANCELACION,EVIDENCIA_APROBADA,EVIDENCIA_RECHAZADA";
    static final String PROVIDER_PREFIX = "PROVEEDOR_";
    static final int MAX_TITLE = 65;
    static final int MAX_BODY = 180;

    private final boolean alertsEnabled;
    private final Set<String> serviceUpdateTypes;

    public FallbackPushMapper(
            @Value("${findu.push.alerts-enabled:false}") boolean alertsEnabled,
            @Value("${findu.push.service-update-types:" + DEFAULT_SERVICE_UPDATE_TYPES + "}") String serviceUpdateTypes) {
        this.alertsEnabled = alertsEnabled;
        this.serviceUpdateTypes = parseTypes(serviceUpdateTypes);
    }

    /** Parte por coma, trim, upper-case, descarta vacios; si queda vacia usa el default. */
    static Set<String> parseTypes(String raw) {
        Set<String> parsed = toSet(raw);
        return parsed.isEmpty() ? toSet(DEFAULT_SERVICE_UPDATE_TYPES) : Collections.unmodifiableSet(parsed);
    }

    private static Set<String> toSet(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(java.util.Locale.ROOT))
                .forEach(out::add);
        return out;
    }

    public Optional<NotificationMessage> map(JsonNode root) {
        try {
            if (root == null || root.isMissingNode() || root.isNull()) {
                return Optional.empty();
            }
            String eventType = root.path("eventType").asText("").trim();
            String eventId = text(root.path("eventId"));
            String targetUserId = root.path("targetUserId").asText("");
            JsonNode rawPayload = root.path("payload");
            JsonNode payload = rawPayload.isObject() ? rawPayload : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();

            if (targetUserId.isBlank()) {
                log.warn("[FALLBACK] Evento {} sin targetUserId; se descarta.", eventType);
                return Optional.empty();
            }

            if ("CHAT_MESSAGE".equalsIgnoreCase(eventType) || payload.has("content")) {
                return Optional.of(chat(payload, targetUserId, eventId));
            }

            if (!targetUserId.startsWith(PROVIDER_PREFIX)) {
                log.debug("[FALLBACK] targetUserId={} no es PROVEEDOR_*; sin push (eventType={}).", targetUserId, eventType);
                return Optional.empty();
            }

            String upperType = eventType.toUpperCase(java.util.Locale.ROOT);
            String tipoEvento = "";
            switch (upperType) {
                case "OFFER_RECEIVED" -> tipoEvento = text(payload.path("tipoEvento"));
                case "SERVICE_UPDATE" -> {
                    tipoEvento = firstText(payload, "tipoEvento", "action", "tipo").toUpperCase(java.util.Locale.ROOT);
                    if (!serviceUpdateTypes.contains(tipoEvento)) {
                        log.info("[FALLBACK] SERVICE_UPDATE tipoEvento={} fuera de la lista blanca; sin push.", tipoEvento);
                        return Optional.empty();
                    }
                }
                case "SYSTEM_ALERT" -> {
                    if (!alertsEnabled) {
                        log.info("[FALLBACK] SYSTEM_ALERT omitido (findu.push.alerts-enabled=false).");
                        return Optional.empty();
                    }
                    tipoEvento = text(payload.path("tipoEvento"));
                }
                default -> {
                    log.debug("[FALLBACK] eventType={} sin push.", eventType);
                    return Optional.empty();
                }
            }
            return Optional.of(generic(upperType, tipoEvento, payload, targetUserId, eventId));
        } catch (Exception e) {
            log.error("[FALLBACK] Error mapeando evento: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private NotificationMessage chat(JsonNode payload, String targetUserId, String eventId) {
        Map<String, String> params = new HashMap<>();
        params.put("type", "TICKET_CHAT");
        params.put("ticketCode", payload.path("ticketCode").asText("N/A"));
        params.put("mensaje", payload.path("content").asText(""));
        params.put("senderName", payload.path("senderName").asText("Soporte FindU"));
        putIfPresent(params, "eventId", eventId);
        return build("TICKET_MESSAGE_RECEIVED", targetUserId, params);
    }

    private NotificationMessage generic(String eventType, String tipoEvento, JsonNode payload,
                                        String targetUserId, String eventId) {
        Map<String, String> params = new HashMap<>();
        params.put("type", eventType);
        putIfPresent(params, "tipoEvento", tipoEvento);
        putIfPresent(params, "solicitudId", text(payload.path("solicitudId")));
        putIfPresent(params, "ofertaId", text(payload.path("ofertaId")));
        String servicio = text(payload.path("servicioNombre"));
        putIfPresent(params, "servicioNombre", servicio);
        putIfPresent(params, "eventId", eventId);

        String title = firstText(payload, "titulo", "title");
        String body = firstText(payload, "mensaje", "message", "body");
        String key = "OFFER_RECEIVED".equals(eventType) || "SYSTEM_ALERT".equals(eventType) ? eventType : tipoEvento;
        if (title.isEmpty()) {
            title = defaultTitle(key);
        }
        if (body.isEmpty()) {
            body = defaultBody(key, servicio, text(payload.path("municipioNombre")));
        }
        params.put("pushTitle", truncate(title, MAX_TITLE));
        params.put("pushBody", truncate(body, MAX_BODY));
        return build("EVENT_PUSH", targetUserId, params);
    }

    private static String defaultTitle(String key) {
        return switch (key) {
            case "NUEVA_SOLICITUD" -> "Nueva solicitud disponible";
            case "OFFER_RECEIVED" -> "Recibiste una nueva oferta";
            case "OFERTA_ACEPTADA" -> "¡Oferta aceptada!";
            case "SOLICITUD_ACTUALIZADA" -> "El cliente modificó una solicitud";
            case "ALARMA_SERVICIO_1H" -> "Tu servicio comienza en 1 hora";
            case "AMONESTACION_GENERADA", "RESTRICCION_ACTIVADA" -> "Aviso importante sobre tu cuenta";
            case "SYSTEM_ALERT" -> "Tienes una alerta pendiente";
            default -> "FindU";
        };
    }

    private static String defaultBody(String key, String servicio, String municipio) {
        return switch (key) {
            case "NUEVA_SOLICITUD" -> {
                if (servicio.isEmpty()) {
                    yield "Hay una solicitud nueva para ti.";
                }
                yield municipio.isEmpty() ? servicio : servicio + " en " + municipio;
            }
            case "OFFER_RECEIVED", "AMONESTACION_GENERADA", "RESTRICCION_ACTIVADA" -> "Revisa los detalles en la app.";
            case "OFERTA_ACEPTADA" -> "Tu oferta para " + (servicio.isEmpty() ? "el servicio" : servicio)
                    + " fue aceptada. Revisa Mis Trabajos.";
            case "SOLICITUD_ACTUALIZADA" -> "Revisa los cambios en la app.";
            case "ALARMA_SERVICIO_1H" -> "Prepárate para tu próximo servicio.";
            case "SYSTEM_ALERT" -> "Ábrela en la app para revisarla.";
            default -> "Tienes una novedad en tu cuenta.";
        };
    }

    private NotificationMessage build(String templateCode, String recipient, Map<String, String> params) {
        return NotificationMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .type("PUSH")
                .recipient(recipient)
                .templateCode(templateCode)
                .language("es")
                .params(params)
                .requiresConnection(false)
                .createdAt(Instant.now().toString())
                .build();
    }

    private static void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    /** Texto escalar no vacio; "" si es ausente, objeto o arreglo. */
    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static String firstText(JsonNode payload, String... keys) {
        for (String k : keys) {
            String v = text(payload.path(k));
            if (!v.isEmpty()) {
                return v;
            }
        }
        return "";
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}

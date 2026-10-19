package com.findu.dispatcher.presentation.controller;

import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.model.EventType;
import com.findu.dispatcher.domain.model.TipoEventoCanal;
import com.findu.dispatcher.infrastructure.adapter.rabbitmq.TransactionalNotificationProducer;
import com.findu.dispatcher.presentation.controller.dto.NotificationMessagePayload;
import com.findu.dispatcher.service.EventDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationDispatchController {

    private static final Set<String> CANALES_TRANSACCIONALES = Set.of("SMS", "EMAIL", "PUSH", "WHATSAPP");

    private final EventDispatcherService eventDispatcherService;
    private final TransactionalNotificationProducer transactionalNotificationProducer;

    public record DispatchNotificationRequest(
            String type,
            String eventType,
            String recipient,
            String targetUserId,
            String templateCode,
            String language,
            Map<String, Object> params
    ) {
        public DispatchNotificationRequest(String type, String recipient, String templateCode, String language, Map<String, Object> params) {
            this(type, null, recipient, null, templateCode, language, params);
        }

        public String effectiveRecipient() {
            if (recipient != null && !recipient.isBlank()) return recipient.trim();
            if (targetUserId != null && !targetUserId.isBlank()) return targetUserId.trim();
            return null;
        }

        public String effectiveType() {
            if (type != null && !type.isBlank()) return type.trim();
            if (eventType != null && !eventType.isBlank()) return eventType.trim();
            return null;
        }
    }

    @PostMapping(value = {"/dispatchNotification", "/api/v1/canales/dispatchNotification"})
    public Mono<ResponseEntity<Void>> dispatchNotification(@RequestBody DispatchNotificationRequest request) {
        String recipient = request.effectiveRecipient();
        String type = request.effectiveType();

        log.info("Notificación recibida para despacho: recipient={}, templateCode={}, type={}",
                recipient, request.templateCode(), type);

        if (recipient == null || recipient.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Bifurcación: notificaciones transaccionales (templateCode + type de canal) van al exchange
        // findu.notifications que consume el processor; los eventos de presencia siguen por EventDispatcherService.
        if (esNotificacionTransaccional(request)) {
            NotificationMessagePayload payload = construirPayloadTransaccional(request, recipient, type);
            return transactionalNotificationProducer.publish(payload)
                    .then(Mono.just(ResponseEntity.ok().build()));
        }

        return despacharEventoPresencia(request, recipient);
    }

    private boolean esNotificacionTransaccional(DispatchNotificationRequest request) {
        if (request.templateCode() == null || request.templateCode().isBlank()) {
            return false;
        }
        String type = request.effectiveType();
        if (type == null) {
            return false;
        }
        return CANALES_TRANSACCIONALES.contains(type.trim().toUpperCase());
    }

    private NotificationMessagePayload construirPayloadTransaccional(DispatchNotificationRequest request, String recipient, String type) {
        Map<String, String> params = new LinkedHashMap<>();
        if (request.params() != null) {
            request.params().forEach((k, v) -> {
                if (v != null) {
                    params.put(k, String.valueOf(v));
                }
            });
        }
        String language = (request.language() == null || request.language().isBlank()) ? "es" : request.language();
        return new NotificationMessagePayload(
                UUID.randomUUID().toString(),
                type != null ? type.trim().toUpperCase() : "PUSH",
                recipient,
                request.templateCode(),
                language,
                params,
                false,
                Instant.now().toString()
        );
    }

    private Mono<ResponseEntity<Void>> despacharEventoPresencia(DispatchNotificationRequest request, String recipient) {
        Map<String, Object> payload = request.params() != null ? new HashMap<>(request.params()) : new HashMap<>();
        payload.put("templateCode", request.templateCode());
        payload.put("type", request.effectiveType());
        if (!payload.containsKey("tipoEvento") && request.templateCode() != null) {
            payload.put("tipoEvento", request.templateCode());
        }

        String canalId;
        if (recipient.startsWith("PROVEEDOR_") || recipient.startsWith("CLIENTE_")
                || recipient.startsWith("AGENTE_") || recipient.startsWith("SOPORTE_")) {
            canalId = recipient;
        } else {
            canalId = "CLIENTE_" + recipient;
        }

        EventType eventType = EventType.SERVICE_UPDATE;
        String typeStr = (request.effectiveType() != null ? request.effectiveType() : "").toUpperCase();
        if (typeStr.contains("CHAT") || typeStr.contains("MESSAGE")) {
            eventType = EventType.CHAT_MESSAGE;
        } else if (typeStr.contains("OFFER") || typeStr.contains("OFERTA")) {
            eventType = EventType.OFFER_RECEIVED;
        } else if (typeStr.contains("ALERT")) {
            eventType = EventType.SYSTEM_ALERT;
        }

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                eventType,
                recipient,
                payload
        );

        TipoEventoCanal tipoEvento = TipoEventoCanal.MENSAJE;
        if (eventType == EventType.OFFER_RECEIVED) {
            tipoEvento = TipoEventoCanal.OFERTA;
        } else if (eventType == EventType.CHAT_MESSAGE) {
            tipoEvento = TipoEventoCanal.MENSAJE;
        } else {
            tipoEvento = TipoEventoCanal.EVENTO_SISTEMA;
        }

        return eventDispatcherService.dispatch(canalId, envelope, tipoEvento)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}

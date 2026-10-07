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
            String recipient,
            String templateCode,
            String language,
            Map<String, Object> params
    ) {}

    @PostMapping(value = {"/dispatchNotification", "/api/v1/canales/dispatchNotification"})
    public Mono<ResponseEntity<Void>> dispatchNotification(@RequestBody DispatchNotificationRequest request) {
        log.info("Notificación recibida para despacho: recipient={}, templateCode={}, type={}",
                request.recipient(), request.templateCode(), request.type());

        String recipient = request.recipient();
        if (recipient == null || recipient.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Bifurcación: notificaciones transaccionales (templateCode + type de canal) van al exchange
        // findu.notifications que consume el processor; los eventos de presencia siguen por EventDispatcherService.
        if (esNotificacionTransaccional(request)) {
            NotificationMessagePayload payload = construirPayloadTransaccional(request);
            return transactionalNotificationProducer.publish(payload)
                    .then(Mono.just(ResponseEntity.ok().build()));
        }

        return despacharEventoPresencia(request, recipient);
    }

    private boolean esNotificacionTransaccional(DispatchNotificationRequest request) {
        if (request.templateCode() == null || request.templateCode().isBlank()) {
            return false;
        }
        if (request.type() == null) {
            return false;
        }
        return CANALES_TRANSACCIONALES.contains(request.type().trim().toUpperCase());
    }

    private NotificationMessagePayload construirPayloadTransaccional(DispatchNotificationRequest request) {
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
                request.type().trim().toUpperCase(),
                request.recipient(),
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
        payload.put("type", request.type());
        if (!payload.containsKey("tipoEvento") && request.templateCode() != null) {
            payload.put("tipoEvento", request.templateCode());
        }

        String canalId;
        if (recipient.startsWith("PROVEEDOR_")) {
            canalId = recipient;
        } else if (recipient.startsWith("CLIENTE_")) {
            canalId = recipient;
        } else {
            canalId = "CLIENTE_" + recipient;
        }

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EventType.OFFER_RECEIVED,
                recipient,
                payload
        );

        TipoEventoCanal tipoEvento = TipoEventoCanal.MENSAJE;
        if (payload.containsKey("tipoEvento")) {
            String ev = String.valueOf(payload.get("tipoEvento"));
            if (ev.contains("OFERTA")) {
                tipoEvento = TipoEventoCanal.OFERTA;
            } else if (ev.contains("SOLICITUD")) {
                tipoEvento = TipoEventoCanal.EVENTO_SISTEMA;
            }
        }

        return eventDispatcherService.dispatch(canalId, envelope, tipoEvento)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}

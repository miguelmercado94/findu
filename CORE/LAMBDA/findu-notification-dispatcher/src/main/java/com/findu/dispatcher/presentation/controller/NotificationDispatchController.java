package com.findu.dispatcher.presentation.controller;

import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.model.EventType;
import com.findu.dispatcher.domain.model.TipoEventoCanal;
import com.findu.dispatcher.service.EventDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationDispatchController {

    private final EventDispatcherService eventDispatcherService;

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

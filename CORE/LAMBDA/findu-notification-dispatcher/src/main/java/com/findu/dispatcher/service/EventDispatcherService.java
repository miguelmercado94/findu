package com.findu.dispatcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.*;
import com.findu.dispatcher.domain.port.EventFallbackPort;
import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import com.findu.dispatcher.domain.port.input.GestionCanalUseCase;
import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDispatcherService {

    private final SessionRegistryPort sessionRegistry;
    private final RedisPubSubPort redisPubSubPort;
    private final EventFallbackPort eventFallbackPort;
    private final GestionCanalUseCase gestionCanalUseCase;
    private final ObjectMapper objectMapper;
    private final RecipientResolver recipientResolver;

    /**
     * Motor de enrutamiento y registro de canales NoSQL.
     */
    public Mono<Void> dispatch(String canalId, EventEnvelope<?> envelope, TipoEventoCanal tipoEvento) {
        String targetUserId = recipientResolver.toCanonical(envelope.targetUserId());

        if (targetUserId == null || targetUserId.isBlank()) {
            log.warn("Evento sin targetUserId omitido. EventId: {}", envelope.eventId());
            return Mono.empty();
        }

        // 1. Crear el sub-JSON del evento para el historial del canal
        EventoCanal eventoCanal = EventoCanal.of(
                tipoEvento != null ? tipoEvento : TipoEventoCanal.MENSAJE,
                targetUserId,
                envelope.payload()
        );

        // 2. Persistir evento en la colección NoSQL del canal y procesar entrega
        return gestionCanalUseCase.agregarEvento(canalId, eventoCanal)
                .onErrorResume(err -> {
                    log.info("El canal {} no estaba registrado: {}. Abriendo canal automáticamente.", canalId, err.getMessage());
                    return gestionCanalUseCase.abrirCanal(canalId, TipoCanal.CANAL_1_OFERTAS, java.util.List.of(new Interoperante(targetUserId, "USER")));
                })
                .flatMap(canal -> {
                    boolean estaInactivo = canal != null && canal.getEstado() == EstadoCanal.INACTIVO;
                    return procesarEntrega(targetUserId, envelope, estaInactivo);
                });
    }

    private Mono<Void> procesarEntrega(String targetUserId, EventEnvelope<?> envelope, boolean canalInactivo) {
        // Si el canal está marcado como INACTIVO (dispositivo bloqueado por scheduler), enviar SIEMPRE a Push FCM
        if (canalInactivo) {
            log.info("Canal INACTIVO detectado (dispositivo bloqueado). Disparando notificación flotante (FCM). targetUserId={}", targetUserId);
            return eventFallbackPort.sendToFallbackQueue(envelope);
        }

        // 1. ¿Usuario conectado en ESTE nodo local?
        if (sessionRegistry.hasLocalSession(targetUserId)) {
            log.info("Usuario {} en socket local. Despachando por WebSocket.", targetUserId);
            return serializeEnvelope(envelope)
                    .flatMap(json -> sessionRegistry.sendToUserLocally(targetUserId, json));
        }

        // 2. ¿Usuario conectado en OTRO nodo del clúster?
        return redisPubSubPort.isUserOnlineInCluster(targetUserId)
                .flatMap(isOnline -> {
                    if (Boolean.TRUE.equals(isOnline)) {
                        log.info("Usuario {} ONLINE en clúster. Publicando en Redis Pub/Sub.", targetUserId);
                        return serializeEnvelope(envelope)
                                .flatMap(json -> redisPubSubPort.publishEventToCluster(targetUserId, json));
                    } else {
                        // 3. Usuario OFFLINE: Enrutar a la cola Fallback (RabbitMQ -> notification-processor -> FCM)
                        log.info("Usuario {} OFFLINE. Enrutando a cola Fallback (FCM Push).", targetUserId);
                        return eventFallbackPort.sendToFallbackQueue(envelope);
                    }
                });
    }

    public Mono<Void> dispatch(EventEnvelope<?> envelope) {
        learnAliasFromPayload(envelope);
        String canonical = recipientResolver.toCanonical(envelope.targetUserId());
        // El canalId se deriva del canónico para que ACTIVO/INACTIVO se evalúe sobre el mismo canal
        // del proveedor (hallazgo #2/#10). El eventId sólo es último recurso.
        String canalId = (canonical != null && !canonical.isBlank()) ? canonical : envelope.eventId();
        return dispatch(canalId, envelope, TipoEventoCanal.MENSAJE);
    }

    /**
     * Aprende el alias {@code aliasRecipient -> canonicalRecipient} que core incluye en el payload.
     * Defensivo: sólo actúa si el payload es un Map con ambas claves String; en otro caso no aprende
     * nada (comportamiento base). {@link RecipientResolver#learnAlias} es idempotente.
     */
    private void learnAliasFromPayload(EventEnvelope<?> envelope) {
        if (envelope.payload() instanceof Map<?, ?> p) {
            Object canon = p.get("canonicalRecipient");
            Object alias = p.get("aliasRecipient");
            if (canon instanceof String c && alias instanceof String a) {
                recipientResolver.learnAlias(a, c);
            }
        }
    }

    private Mono<String> serializeEnvelope(EventEnvelope<?> envelope) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(envelope));
    }
}

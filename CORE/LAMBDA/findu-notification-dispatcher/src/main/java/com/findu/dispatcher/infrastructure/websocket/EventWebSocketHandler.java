package com.findu.dispatcher.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import com.findu.dispatcher.infrastructure.security.WebSocketAuthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventWebSocketHandler implements WebSocketHandler {

    private static final int DEFAULT_MAX_MISSED_PONGS = 3;

    private final WebSocketAuthService authService;
    private final SessionRegistryPort sessionRegistry;
    private final RedisPubSubPort redisPubSubPort;
    private final ObjectMapper objectMapper;
    private final RecipientResolver recipientResolver;

    @Value("${findu.websocket.ping-interval-seconds:30}")
    private long pingIntervalSeconds;

    @Value("${eureka.instance.instance-id:node-1}")
    private String nodeId;

    /** Ticks consecutivos sin trafico entrante tras los cuales se cierra la sesion (zombi). */
    @Value("${findu.websocket.max-missed-pongs:3}")
    private int maxMissedPongs;

    @PostConstruct
    void init() {
        if (maxMissedPongs < 1) {
            log.warn("findu.websocket.max-missed-pongs={} invalido (<1); se usa {}", maxMissedPongs, DEFAULT_MAX_MISSED_PONGS);
            maxMissedPongs = DEFAULT_MAX_MISSED_PONGS;
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return authService.authorize(session.getHandshakeInfo().getUri())
                .flatMap(decision -> decision.accepted()
                        ? Mono.just(decision.channel())
                        : Mono.<String>error(new IllegalArgumentException("handshake rechazado: " + decision.reason())))
                .onErrorResume(ex -> {
                    log.warn("Rechazando conexión WebSocket por autenticación fallida: {}", ex.getMessage());
                    return session.close(CloseStatus.BAD_DATA).then(Mono.<String>empty());
                })
                .flatMap(userId -> {
                    // Normalizar a la clave canónica única: registro, presencia y baja usan ESA misma clave.
                    String canonicalUserId = recipientResolver.toCanonical(userId);
                    log.info("Conexión WebSocket autenticada. userId={}, canonicalUserId={}, sessionId={}", userId, canonicalUserId, session.getId());

                    // 1. Registrar sesión local y presencia en el clúster (clave canónica única)
                    sessionRegistry.registerSession(canonicalUserId, session);
                    Mono<Void> registerPresence = redisPubSubPort.registerPresence(canonicalUserId, nodeId);

                    // Ticks consecutivos sin ningún frame entrante (pong crudo, PONG o cualquier mensaje).
                    AtomicInteger missed = new AtomicInteger();

                    // 2. Heartbeat Ping-Pong periódico para evitar timeouts
                    Flux<WebSocketMessage> pingFlux = Flux.interval(Duration.ofSeconds(pingIntervalSeconds))
                            .flatMap(tick -> {
                                if (session.isOpen()) {
                                    try {
                                        String pingJson = objectMapper.writeValueAsString(EventEnvelope.ping());
                                        return Mono.just(session.textMessage(pingJson));
                                    } catch (Exception e) {
                                        return Mono.empty();
                                    }
                                }
                                return Mono.empty();
                            });

                    // 3. Listener de mensajes entrantes desde el cliente (PONG o mensajes de cliente)
                    Mono<Void> receiveMono = session.receive()
                            .doOnNext(msg -> {
                                missed.set(0);
                                log.trace("Mensaje entrante de WebSocket (userId={})", userId);
                            })
                            .doOnError(err -> log.error("Error en flujo de recepción (userId={}): {}", userId, err.getMessage()))
                            .then();

                    // Send flux para emitir PINGs continuos
                    Mono<Void> sendPingMono = session.send(pingFlux);

                    // 4. Watchdog de zombis: cierra la sesión tras maxMissedPongs ticks sin tráfico entrante.
                    Mono<Void> watchdogMono = Flux.interval(Duration.ofSeconds(pingIntervalSeconds))
                            .filter(tick -> missed.incrementAndGet() >= maxMissedPongs)
                            .next()
                            .flatMap(tick -> {
                                log.warn("Sin tráfico entrante tras {} ticks de {}s; cerrando sesión zombi (canonicalUserId={}, sessionId={})",
                                        maxMissedPongs, pingIntervalSeconds, canonicalUserId, session.getId());
                                return session.close(CloseStatus.GOING_AWAY)
                                        .timeout(Duration.ofSeconds(5))
                                        .onErrorResume(e -> {
                                            log.warn("No se pudo cerrar la sesión zombi {}: {}", session.getId(), e.getMessage());
                                            return Mono.<Void>empty();
                                        });
                            });

                    // La primera señal (cierre del cliente, error o watchdog) termina la cadena,
                    // cancela las demás y dispara doFinally de inmediato.
                    return registerPresence
                            .then(Mono.firstWithSignal(receiveMono, sendPingMono, watchdogMono).then())
                            .doFinally(signalType -> {
                                log.info("Cerrando sesión WebSocket (canonicalUserId={}, signal={})", canonicalUserId, signalType);
                                sessionRegistry.unregisterSession(canonicalUserId, session.getId());
                                // Si queda otra sesión local abierta del mismo usuario, la presencia en el clúster se conserva.
                                if (!sessionRegistry.hasLocalSession(canonicalUserId)) {
                                    redisPubSubPort.removePresence(canonicalUserId, nodeId)
                                            .onErrorResume(e -> {
                                                log.warn("No se pudo quitar la presencia de {}: {}", canonicalUserId, e.getMessage());
                                                return Mono.<Void>empty();
                                            })
                                            .subscribe();
                                }
                            })
                            .onErrorResume(ex -> {
                                log.warn("Sesión WebSocket terminada con error (canonicalUserId={}): {}", canonicalUserId, ex.getMessage());
                                return Mono.<Void>empty();
                            });
                });
    }
}

package com.findu.dispatcher.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import com.findu.dispatcher.infrastructure.security.JwtValidator;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class EventWebSocketHandler implements WebSocketHandler {

    private final JwtValidator jwtValidator;
    private final SessionRegistryPort sessionRegistry;
    private final RedisPubSubPort redisPubSubPort;
    private final ObjectMapper objectMapper;

    @Value("${findu.websocket.ping-interval-seconds:30}")
    private long pingIntervalSeconds;

    @Value("${eureka.instance.instance-id:node-1}")
    private String nodeId;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return jwtValidator.extractAndValidateUserId(session.getHandshakeInfo().getUri())
                .flatMap(userId -> {
                    log.info("Conexión WebSocket autenticada. userId={}, sessionId={}", userId, session.getId());

                    // 1. Registrar sesión local y presencia en el clúster
                    sessionRegistry.registerSession(userId, session);
                    Mono<Void> registerPresence = redisPubSubPort.registerPresence(userId, nodeId);

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
                                String payload = msg.getPayloadAsText();
                                log.trace("Mensaje entrante de WebSocket (userId={}): {}", userId, payload);
                            })
                            .doOnError(err -> log.error("Error en flujo de recepción (userId={}): {}", userId, err.getMessage()))
                            .then();

                    // Send flux para emitir PINGs continuos
                    Mono<Void> sendPingMono = session.send(pingFlux);

                    return registerPresence
                            .then(Mono.zip(receiveMono, sendPingMono).then())
                            .doFinally(signalType -> {
                                log.info("Cerrando sesión WebSocket (userId={}, signal={})", userId, signalType);
                                sessionRegistry.unregisterSession(userId, session.getId());
                                redisPubSubPort.removePresence(userId, nodeId).subscribe();
                            });
                })
                .onErrorResume(ex -> {
                    log.warn("Rechazando conexión WebSocket por autenticación fallida: {}", ex.getMessage());
                    return session.close(CloseStatus.BAD_DATA);
                });
    }
}

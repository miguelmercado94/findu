package com.findu.dispatcher.infrastructure.adapter.memory;

import com.findu.dispatcher.domain.port.SessionRegistryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LocalSessionRegistryAdapter implements SessionRegistryPort {

    // Mapa thread-safe: userId -> Set de WebSocketSessions activas en este nodo
    private final ConcurrentHashMap<String, Set<WebSocketSession>> localSessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String userId, WebSocketSession session) {
        localSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("Sesión WebSocket registrada localmente. userId={}, sessionId={}, totalLocalSockets={}",
                userId, session.getId(), localSessions.get(userId).size());
    }

    @Override
    public void unregisterSession(String userId, String sessionId) {
        Set<WebSocketSession> sessions = localSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(s -> s.getId().equals(sessionId));
            if (sessions.isEmpty()) {
                localSessions.remove(userId);
            }
            log.info("Sesión WebSocket removida localmente. userId={}, sessionId={}", userId, sessionId);
        }
    }

    @Override
    public Set<WebSocketSession> getSessions(String userId) {
        return localSessions.getOrDefault(userId, Collections.emptySet());
    }

    @Override
    public boolean hasLocalSession(String userId) {
        Set<WebSocketSession> sessions = localSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    @Override
    public Mono<Void> sendToUserLocally(String userId, String messageJson) {
        Set<WebSocketSession> sessions = getSessions(userId);
        if (sessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sessions)
                .flatMap(session -> {
                    if (session.isOpen()) {
                        WebSocketMessage msg = session.textMessage(messageJson);
                        return session.send(Mono.just(msg))
                                .doOnError(err -> log.error("Error al enviar mensaje a la sesión {}: {}", session.getId(), err.getMessage()));
                    }
                    return Mono.empty();
                })
                .then();
    }
}

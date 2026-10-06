package com.findu.dispatcher.domain.port;

import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface SessionRegistryPort {
    void registerSession(String userId, WebSocketSession session);
    void unregisterSession(String userId, String sessionId);
    Set<WebSocketSession> getSessions(String userId);
    boolean hasLocalSession(String userId);
    Mono<Void> sendToUserLocally(String userId, String messageJson);
}

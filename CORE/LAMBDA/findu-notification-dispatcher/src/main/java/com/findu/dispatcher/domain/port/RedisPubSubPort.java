package com.findu.dispatcher.domain.port;

import reactor.core.publisher.Mono;

public interface RedisPubSubPort {
    Mono<Void> registerPresence(String userId, String nodeId);
    Mono<Void> removePresence(String userId, String nodeId);
    Mono<Boolean> isUserOnlineInCluster(String userId);
    Mono<Void> publishEventToCluster(String userId, String eventJson);
}

package com.findu.dispatcher.domain.port;

import com.findu.dispatcher.domain.model.EventEnvelope;
import reactor.core.publisher.Mono;

public interface EventFallbackPort {
    Mono<Void> sendToFallbackQueue(EventEnvelope<?> event);
}

package com.findu.security.infrastructure.adapter.cache;

import com.findu.security.application.port.output.cache.TokenRevocationCachePort;
import com.findu.security.domain.model.CachedTokenSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Sin caché: siempre delega a Dynamo (consultas devuelven false).
 */
@Component
@ConditionalOnProperty(name = "findu.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTokenRevocationCacheAdapter implements TokenRevocationCachePort {

    @Override
    public Mono<Boolean> isAccessRevokedInCache(String accessJwt) {
        return Mono.just(false);
    }

    @Override
    public Mono<Boolean> isRefreshRevokedInCache(String refreshJwt) {
        return Mono.just(false);
    }

    @Override
    public Mono<Void> putRevokedSession(CachedTokenSession session, long ttlEpochSeconds) {
        return Mono.empty();
    }

    @Override
    public Mono<Long> removeExpiredRevokedEntries() {
        return Mono.just(0L);
    }
}

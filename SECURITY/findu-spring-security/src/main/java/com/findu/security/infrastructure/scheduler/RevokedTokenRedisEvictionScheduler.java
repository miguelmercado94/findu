package com.findu.security.infrastructure.scheduler;

import com.findu.security.application.port.output.cache.TokenRevocationCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Proceso aislado (migrable a Lambda): borra de Redis sesiones revocadas cuyo JWT ya expiró.
 */
@Component
@ConditionalOnProperty(name = "findu.redis.enabled", havingValue = "true")
public class RevokedTokenRedisEvictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenRedisEvictionScheduler.class);

    private final TokenRevocationCachePort tokenRevocationCachePort;

    public RevokedTokenRedisEvictionScheduler(TokenRevocationCachePort tokenRevocationCachePort) {
        this.tokenRevocationCachePort = tokenRevocationCachePort;
    }

    @Scheduled(fixedDelayString = "${findu.redis.eviction-interval-ms:3600000}", initialDelayString = "${findu.redis.eviction-interval-ms:3600000}")
    public void evictExpiredRevokedEntries() {
        tokenRevocationCachePort.removeExpiredRevokedEntries()
                .subscribe(
                        count -> log.info("Redis: eliminadas {} entradas de revocación expiradas", count),
                        e -> log.warn("Redis eviction job falló: {}", e.toString())
                );
    }
}

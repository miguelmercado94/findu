package com.findu.security.infrastructure.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.port.output.cache.TokenRevocationCachePort;
import com.findu.security.application.service.JwtService;
import com.findu.security.config.properties.RedisCacheProperties;
import com.findu.security.domain.model.CachedTokenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Caché Redis reactiva para revocación. Errores y timeouts se registran y se traducen en "miss" (consultar Dynamo).
 */
@Component
@ConditionalOnProperty(name = "findu.redis.enabled", havingValue = "true")
public class RedisTokenRevocationCacheAdapter implements TokenRevocationCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenRevocationCacheAdapter.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisCacheProperties properties;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisTokenRevocationCacheAdapter(
            @Qualifier("finduReactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
            RedisCacheProperties properties,
            JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.jwtService = jwtService;
    }

    private Duration commandTimeout() {
        int ms = properties.commandTimeoutMs() > 0 ? properties.commandTimeoutMs() : 300;
        return Duration.ofMillis(ms);
    }

    private String prefix() {
        String p = properties.keyPrefix();
        return (p == null || p.isBlank()) ? "findu:revoked:" : p;
    }

    @Override
    public Mono<Boolean> isAccessRevokedInCache(String accessJwt) {
        if (accessJwt == null || accessJwt.isBlank()) {
            return Mono.just(false);
        }
        String key = RedisTokenCacheKeys.accessKey(prefix(), accessJwt);
        return redisTemplate.opsForValue().get(key)
                .map(v -> v != null && !v.isBlank())
                .defaultIfEmpty(false)
                .timeout(commandTimeout())
                .onErrorResume(e -> {
                    log.warn("Redis isAccessRevokedInCache timeout/error: {}", e.toString());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Boolean> isRefreshRevokedInCache(String refreshJwt) {
        if (refreshJwt == null || refreshJwt.isBlank()) {
            return Mono.just(false);
        }
        String key = RedisTokenCacheKeys.refreshKey(prefix(), refreshJwt);
        return redisTemplate.opsForValue().get(key)
                .map(v -> v != null && !v.isBlank())
                .defaultIfEmpty(false)
                .timeout(commandTimeout())
                .onErrorResume(e -> {
                    log.warn("Redis isRefreshRevokedInCache timeout/error: {}", e.toString());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Void> putRevokedSession(CachedTokenSession session, long ttlEpochSeconds) {
        if (session == null || session.jwt() == null || session.jwt().isBlank()) {
            return Mono.empty();
        }
        long now = Instant.now().getEpochSecond();
        long ttlSec = ttlEpochSeconds - now;
        if (ttlSec <= 0) {
            log.debug("Redis putRevokedSession skipped: TTL no positivo");
            return Mono.empty();
        }
        Duration ttl = Duration.ofSeconds(ttlSec);
        String json;
        try {
            json = objectMapper.writeValueAsString(session);
        } catch (Exception e) {
            log.warn("Redis putRevokedSession serialize failed", e);
            return Mono.empty();
        }
        String accessKey = RedisTokenCacheKeys.accessKey(prefix(), session.jwt());
        Mono<Void> putAccess = redisTemplate.opsForValue().set(accessKey, json, ttl)
                .timeout(commandTimeout())
                .then();
        if (session.jwtRefresh() != null && !session.jwtRefresh().isBlank()) {
            String refreshKey = RedisTokenCacheKeys.refreshKey(prefix(), session.jwtRefresh());
            Mono<Void> putRefresh = redisTemplate.opsForValue().set(refreshKey, json, ttl)
                    .timeout(commandTimeout())
                    .then();
            return putAccess
                    .then(putRefresh)
                    .onErrorResume(e -> {
                        log.warn("Redis putRevokedSession failed: {}", e.toString());
                        return Mono.empty();
                    });
        }
        return putAccess.onErrorResume(e -> {
            log.warn("Redis putRevokedSession failed: {}", e.toString());
            return Mono.empty();
        });
    }

    @Override
    public Mono<Long> removeExpiredRevokedEntries() {
        String pattern = RedisTokenCacheKeys.accessKeyPattern(prefix());
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        return redisTemplate.scan(options)
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .flatMap(json -> {
                            try {
                                CachedTokenSession s = objectMapper.readValue(json, CachedTokenSession.class);
                                Instant exp = jwtService.getExpiration(s.jwt());
                                if (exp != null && Instant.now().isAfter(exp)) {
                                    Mono<Long> delA = redisTemplate.delete(key);
                                    Mono<Long> delR = (s.jwtRefresh() != null && !s.jwtRefresh().isBlank())
                                            ? redisTemplate.delete(RedisTokenCacheKeys.refreshKey(prefix(), s.jwtRefresh()))
                                            : Mono.just(0L);
                                    return delA.then(delR).thenReturn(1L);
                                }
                            } catch (Exception e) {
                                log.warn("Redis eviction parse/delete key={}: {}", key, e.toString());
                            }
                            return Mono.just(0L);
                        })
                        .defaultIfEmpty(0L)
                        .timeout(commandTimeout())
                        .onErrorResume(e -> {
                            log.warn("Redis eviction key read failed: {}", e.toString());
                            return Mono.just(0L);
                        }), 1)
                .reduce(0L, Long::sum)
                .defaultIfEmpty(0L);
    }
}

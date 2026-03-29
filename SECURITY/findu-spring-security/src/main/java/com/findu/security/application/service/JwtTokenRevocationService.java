package com.findu.security.application.service;

import com.findu.security.application.port.output.cache.TokenRevocationCachePort;
import com.findu.security.application.port.output.persistence.RevokedTokenRepositoryPort;
import com.findu.security.domain.model.CachedTokenSession;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Sesiones JWT: Dynamo como fuente persistente; caché Redis opcional (rápida, no bloqueante ante fallo).
 */
@Service
public class JwtTokenRevocationService {

    private final RevokedTokenRepositoryPort revokedTokenRepositoryPort;
    private final TokenRevocationCachePort tokenRevocationCachePort;
    private final JwtService jwtService;

    public JwtTokenRevocationService(RevokedTokenRepositoryPort revokedTokenRepositoryPort,
                                     TokenRevocationCachePort tokenRevocationCachePort,
                                     JwtService jwtService) {
        this.revokedTokenRepositoryPort = revokedTokenRepositoryPort;
        this.tokenRevocationCachePort = tokenRevocationCachePort;
        this.jwtService = jwtService;
    }

    /**
     * Access: Redis primero; si miss o error, Dynamo. Refresh: idem.
     */
    public Mono<Boolean> isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }
        if (jwtService.isRefreshToken(token)) {
            return tokenRevocationCachePort.isRefreshRevokedInCache(token)
                    .flatMap(cached -> {
                        if (Boolean.TRUE.equals(cached)) {
                            return Mono.just(true);
                        }
                        return revokedTokenRepositoryPort.isRefreshBlocked(token);
                    });
        }
        return tokenRevocationCachePort.isAccessRevokedInCache(token)
                .flatMap(cached -> {
                    if (Boolean.TRUE.equals(cached)) {
                        return Mono.just(true);
                    }
                    return revokedTokenRepositoryPort.isAccessBlocked(token);
                });
    }

    public Mono<Void> registerIssuedPair(String accessJwt, String refreshJwt) {
        if (accessJwt == null || accessJwt.isBlank() || refreshJwt == null || refreshJwt.isBlank()) {
            return Mono.empty();
        }
        long ttl = Math.max(
                jwtService.getExpiration(accessJwt).getEpochSecond(),
                jwtService.getExpiration(refreshJwt).getEpochSecond()
        );
        return revokedTokenRepositoryPort.saveTokenPair(accessJwt, refreshJwt, ttl);
    }

    /**
     * Logout: Dynamo primero; luego Redis con la misma estructura (fallo Redis no revierte logout).
     */
    public Mono<Void> markSessionUnavailable(String accessJwt, String refreshJwtOptional) {
        if (accessJwt == null || accessJwt.isBlank()) {
            return Mono.empty();
        }
        long baseTtl = jwtService.getExpiration(accessJwt).getEpochSecond();
        boolean refreshOk = refreshJwtOptional != null && !refreshJwtOptional.isBlank()
                && jwtService.isValid(refreshJwtOptional);
        final long ttlEpoch = refreshOk
                ? Math.max(baseTtl, jwtService.getExpiration(refreshJwtOptional).getEpochSecond())
                : baseTtl;
        String refreshForCache = refreshOk ? refreshJwtOptional : "";
        return revokedTokenRepositoryPort.markSessionUnavailable(accessJwt, refreshJwtOptional, ttlEpoch)
                .then(Mono.defer(() -> tokenRevocationCachePort.putRevokedSession(
                        new CachedTokenSession(accessJwt, refreshForCache, false),
                        ttlEpoch)));
    }

    public Mono<Void> rotateSession(String oldRefreshJwt, String newAccessJwt, String newRefreshJwt) {
        if (newAccessJwt == null || newAccessJwt.isBlank() || newRefreshJwt == null || newRefreshJwt.isBlank()) {
            return Mono.empty();
        }
        long ttl = Math.max(
                jwtService.getExpiration(newAccessJwt).getEpochSecond(),
                jwtService.getExpiration(newRefreshJwt).getEpochSecond()
        );
        return revokedTokenRepositoryPort.rotateSession(oldRefreshJwt, newAccessJwt, newRefreshJwt, ttl);
    }
}

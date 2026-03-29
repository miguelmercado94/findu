package com.findu.security.application.port.output.cache;

import com.findu.security.domain.model.CachedTokenSession;
import reactor.core.publisher.Mono;

/**
 * Caché opcional de revocación (Redis, ElastiCache, etc.). Implementaciones deben ser no bloqueantes;
 * ante error o timeout el llamador debe consultar el almacén persistente (p. ej. DynamoDB).
 */
public interface TokenRevocationCachePort {

    /**
     * @return {@code true} si la caché confirma access revocado; {@code false} si no hay entrada o falló (consultar Dynamo).
     */
    Mono<Boolean> isAccessRevokedInCache(String accessJwt);

    /**
     * @return {@code true} si la caché confirma refresh revocado; {@code false} si no hay entrada o falló.
     */
    Mono<Boolean> isRefreshRevokedInCache(String refreshJwt);

    /**
     * Guarda el mismo criterio que Dynamo tras logout (típicamente {@code available=false}).
     * TTL hasta el fin de vida útil del token en caché.
     */
    Mono<Void> putRevokedSession(CachedTokenSession session, long ttlEpochSeconds);

    /**
     * Elimina de Redis entradas cuyo access JWT ya expiró (Dynamo sigue siendo fuente legal).
     *
     * @return cantidad de sesiones eliminadas (aprox.)
     */
    Mono<Long> removeExpiredRevokedEntries();
}

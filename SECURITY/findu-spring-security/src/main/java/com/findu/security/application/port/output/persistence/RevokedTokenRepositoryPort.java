package com.findu.security.application.port.output.persistence;

import reactor.core.publisher.Mono;

/**
 * Sesiones de token en almacén (DynamoDB / memoria): par access+refresh con {@code available},
 * y rotación en {@code /refresh}.
 */
public interface RevokedTokenRepositoryPort {

    /**
     * Persiste el par emitido (login/registro/refresh) con {@code available=true}.
     *
     * @param ttlEpochSeconds TTL Dynamo (epoch segundos), típicamente max(exp access, exp refresh)
     */
    Mono<Void> saveTokenPair(String accessJwt, String refreshJwt, long ttlEpochSeconds);

    /**
     * Logout: marca {@code available=false} en la sesión del access; si no hay registro, crea uno revocado
     * (invalida access y, si viene, el refresh en el mismo ítem).
     *
     * @param ttlEpochSeconds TTL del ítem (p. ej. max(exp access, exp refresh))
     */
    Mono<Void> markSessionUnavailable(String accessJwt, String refreshJwtOptional, long ttlEpochSeconds);

    /**
     * {@code true} si el access está revocado (sesión no disponible o lista negra legada).
     */
    Mono<Boolean> isAccessBlocked(String accessJwt);

    /**
     * {@code true} si el refresh pertenece a una sesión con {@code available=false} (o lista negra legada).
     */
    Mono<Boolean> isRefreshBlocked(String refreshJwt);

    /**
     * Sustituye la sesión del refresh antiguo por el nuevo par (rotación).
     */
    Mono<Void> rotateSession(String oldRefreshJwt, String newAccessJwt, String newRefreshJwt, long ttlEpochSeconds);
}

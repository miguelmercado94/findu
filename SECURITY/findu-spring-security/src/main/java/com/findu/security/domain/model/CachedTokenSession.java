package com.findu.security.domain.model;

/**
 * Snapshot de sesión revocada para caché (Redis u otro backend).
 * Tras logout {@code available} es {@code false}; Dynamo conserva la verdad persistente.
 */
public record CachedTokenSession(
        String jwt,
        String jwtRefresh,
        boolean available
) {
}

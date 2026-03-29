package com.findu.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Caché Redis para revocación (opcional). Timeouts cortos: fallo → se consulta Dynamo sin bloquear.
 */
@ConfigurationProperties(prefix = "findu.redis")
public record RedisCacheProperties(
        boolean enabled,
        int commandTimeoutMs,
        long evictionIntervalMs,
        String keyPrefix
) {
}

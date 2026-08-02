package com.findu.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caché in-memory para perfil local (sin Redis).
 * Se activa cuando findu.cache.enabled=false o no está definido.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "findu.cache.enabled", havingValue = "false", matchIfMissing = true)
public class LocalCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "categorias", "categorias-page", "subcategorias",
                "servicios-categoria", "servicios-page", "municipios"
        );
    }
}

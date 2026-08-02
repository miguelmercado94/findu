package com.findu.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuración de caché con Redis (ElastiCache en producción).
 * Solo se activa cuando findu.cache.enabled=true (perfil dev/qa/pdn).
 * En perfil local, se usa ConcurrentMapCacheManager (in-memory) por defecto de Spring.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "findu.cache.enabled", havingValue = "true")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("categorias", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("categorias-page", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("subcategorias", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("servicios-categoria", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("servicios-page", config.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("municipios", config.entryTtl(Duration.ofHours(6)))
                .build();
    }
}

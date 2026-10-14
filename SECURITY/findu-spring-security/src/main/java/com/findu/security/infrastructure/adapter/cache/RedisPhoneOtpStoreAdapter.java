package com.findu.security.infrastructure.adapter.cache;

import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Store Redis del OTP de teléfono. Solo activo cuando findu.redis.enabled=true (perfiles qa/pdn, Railway).
 * Clave: findu:otp:phone:&lt;e164&gt;.
 */
@Component
@ConditionalOnProperty(name = "findu.redis.enabled", havingValue = "true")
public class RedisPhoneOtpStoreAdapter implements PhoneOtpStorePort {

    private static final String KEY_PREFIX = "findu:otp:phone:";
    private static final String VERIFIED_KEY_PREFIX = "findu:otp:verified:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisPhoneOtpStoreAdapter(
            @Qualifier("finduReactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String phoneKey) {
        return KEY_PREFIX + phoneKey;
    }

    private String verifiedKey(String phoneKey) {
        return VERIFIED_KEY_PREFIX + phoneKey;
    }

    @Override
    public Mono<Void> save(String phoneKey, String json, Duration ttl) {
        return redisTemplate.opsForValue().set(key(phoneKey), json, ttl).then();
    }

    @Override
    public Mono<String> find(String phoneKey) {
        return redisTemplate.opsForValue().get(key(phoneKey));
    }

    @Override
    public Mono<Void> delete(String phoneKey) {
        return redisTemplate.opsForValue().delete(key(phoneKey)).then();
    }

    @Override
    public Mono<Void> markPhoneVerified(String phoneKey, Duration ttl) {
        return redisTemplate.opsForValue().set(verifiedKey(phoneKey), "true", ttl).then();
    }

    @Override
    public Mono<Boolean> isPhoneVerified(String phoneKey) {
        return redisTemplate.opsForValue().get(verifiedKey(phoneKey))
                .map(v -> "true".equalsIgnoreCase(v))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> deletePhoneVerified(String phoneKey) {
        return redisTemplate.opsForValue().delete(verifiedKey(phoneKey)).then();
    }
}

package com.findu.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PhoneOtpValidationService {

    private static final String KEY_PREFIX = "findu:otp:verified:";
    private static final String DEFAULT_COUNTRY_CODE = "+57";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Boolean> inMemoryFallback = new ConcurrentHashMap<>();

    public PhoneOtpValidationService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String e164(String phone, String codPhoneInternational) {
        String country = (codPhoneInternational != null && !codPhoneInternational.isBlank())
                ? codPhoneInternational.trim() : DEFAULT_COUNTRY_CODE;
        return country + phone.trim();
    }

    private String key(String e164) {
        return KEY_PREFIX + e164;
    }

    public Mono<Boolean> isPhoneVerified(String phone, String codPhoneInternational) {
        if (phone == null || phone.isBlank()) {
            return Mono.just(false);
        }
        String e164 = e164(phone, codPhoneInternational);
        return redisTemplate.opsForValue().get(key(e164))
                .map(v -> "true".equalsIgnoreCase(v))
                .onErrorResume(e -> {
                    log.warn("Redis error on OTP check, using in-memory fallback for {}: {}", e164, e.getMessage());
                    return Mono.just(inMemoryFallback.getOrDefault(key(e164), false));
                })
                .defaultIfEmpty(inMemoryFallback.getOrDefault(key(e164), false));
    }

    public Mono<Void> consumePhoneVerified(String phone, String codPhoneInternational) {
        if (phone == null || phone.isBlank()) {
            return Mono.empty();
        }
        String e164 = e164(phone, codPhoneInternational);
        inMemoryFallback.remove(key(e164));
        return redisTemplate.opsForValue().delete(key(e164))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    public void markVerifiedInMemoryForTesting(String phone, String codPhoneInternational) {
        String e164 = e164(phone, codPhoneInternational);
        inMemoryFallback.put(key(e164), true);
    }
}

package com.findu.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.gateway.dto.OnboardingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IdempotencyService {

    private static final String KEY_PREFIX = "findu:idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, String> inMemoryFallback = new ConcurrentHashMap<>();

    public IdempotencyService(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String key(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }

    public Mono<OnboardingResponse> getExisting(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(key(idempotencyKey))
                .onErrorResume(e -> {
                    log.warn("Redis error on idempotency lookup, checking in-memory fallback: {}", e.getMessage());
                    String val = inMemoryFallback.get(key(idempotencyKey));
                    return val != null ? Mono.just(val) : Mono.empty();
                })
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, OnboardingResponse.class));
                    } catch (Exception e) {
                        log.warn("Could not deserialize cached onboarding response: {}", e.getMessage());
                        return Mono.empty();
                    }
                });
    }

    public Mono<Void> saveResult(String idempotencyKey, OnboardingResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) {
            return Mono.empty();
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Could not serialize onboarding response for idempotency cache: {}", e.getMessage());
            return Mono.empty();
        }

        inMemoryFallback.put(key(idempotencyKey), json);
        return redisTemplate.opsForValue().set(key(idempotencyKey), json, DEFAULT_TTL)
                .onErrorResume(e -> {
                    log.warn("Could not save idempotency result to Redis: {}", e.getMessage());
                    return Mono.just(true);
                })
                .then();
    }
}

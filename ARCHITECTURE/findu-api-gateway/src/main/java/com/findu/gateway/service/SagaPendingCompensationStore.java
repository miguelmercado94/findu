package com.findu.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.gateway.dto.PendingCompensation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SagaPendingCompensationStore {

    private static final String HASH_KEY = "findu:saga:pending_compensation";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, String> inMemoryFallback = new ConcurrentHashMap<>();

    public SagaPendingCompensationStore(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> savePendingCompensation(PendingCompensation compensation) {
        if (compensation == null) {
            return Mono.empty();
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(compensation);
        } catch (Exception e) {
            log.error("Failed to serialize pending compensation {}: {}", compensation.compensationId(), e.getMessage());
            return Mono.empty();
        }

        inMemoryFallback.put(compensation.compensationId(), json);
        return redisTemplate.opsForHash().put(HASH_KEY, compensation.compensationId(), json)
                .onErrorResume(e -> {
                    log.warn("Redis error on saving pending compensation, kept in memory: {}", e.getMessage());
                    return Mono.just(true);
                })
                .doOnSuccess(v -> log.warn("Saga compensation queued in outbox: id={} action={} targetId={}",
                        compensation.compensationId(), compensation.action(), compensation.targetId()))
                .then();
    }

    public Mono<List<PendingCompensation>> getPendingCompensations() {
        return redisTemplate.opsForHash().values(HASH_KEY)
                .map(obj -> (String) obj)
                .concatWith(Flux.fromIterable(inMemoryFallback.values()))
                .distinct()
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, PendingCompensation.class));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .collectList()
                .onErrorResume(e -> Flux.fromIterable(inMemoryFallback.values())
                        .flatMap(json -> {
                            try {
                                return Mono.just(objectMapper.readValue(json, PendingCompensation.class));
                            } catch (Exception ex) {
                                return Mono.empty();
                            }
                        })
                        .collectList());
    }

    public Mono<Void> removePendingCompensation(String compensationId) {
        if (compensationId == null) {
            return Mono.empty();
        }
        inMemoryFallback.remove(compensationId);
        return redisTemplate.opsForHash().remove(HASH_KEY, compensationId)
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}

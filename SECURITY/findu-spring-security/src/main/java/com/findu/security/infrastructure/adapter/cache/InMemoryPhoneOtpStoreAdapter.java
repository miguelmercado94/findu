package com.findu.security.infrastructure.adapter.cache;

import com.findu.security.application.port.output.persistence.PhoneOtpStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback in-memory del OTP de teléfono para dev y tests (sin Redis).
 * Activo cuando findu.redis.enabled=false o ausente (matchIfMissing=true).
 */
@Component
@ConditionalOnProperty(name = "findu.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryPhoneOtpStoreAdapter implements PhoneOtpStorePort {

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> verifiedStore = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> save(String phoneKey, String json, Duration ttl) {
        long expiresAt = Instant.now().plus(ttl).toEpochMilli();
        store.put(phoneKey, new Entry(json, expiresAt));
        return Mono.empty();
    }

    @Override
    public Mono<String> find(String phoneKey) {
        Entry entry = store.get(phoneKey);
        if (entry == null) {
            return Mono.empty();
        }
        if (Instant.now().toEpochMilli() > entry.expiresAtMillis()) {
            store.remove(phoneKey);
            return Mono.empty();
        }
        return Mono.just(entry.json());
    }

    @Override
    public Mono<Void> delete(String phoneKey) {
        store.remove(phoneKey);
        return Mono.empty();
    }

    @Override
    public Mono<Void> markPhoneVerified(String phoneKey, Duration ttl) {
        long expiresAt = Instant.now().plus(ttl).toEpochMilli();
        verifiedStore.put(phoneKey, expiresAt);
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> isPhoneVerified(String phoneKey) {
        Long expiresAt = verifiedStore.get(phoneKey);
        if (expiresAt == null) {
            return Mono.just(false);
        }
        if (Instant.now().toEpochMilli() > expiresAt) {
            verifiedStore.remove(phoneKey);
            return Mono.just(false);
        }
        return Mono.just(true);
    }

    @Override
    public Mono<Void> deletePhoneVerified(String phoneKey) {
        verifiedStore.remove(phoneKey);
        return Mono.empty();
    }

    private record Entry(String json, long expiresAtMillis) {
    }
}

package com.findu.dispatcher.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Cliente HTTP del endpoint interno de core {@code GET /internal/perfiles/by-auth/{authUserId}}
 * (cabecera X-Internal-Service-Key) que resuelve authUserId -> proveedorId. Timeout corto y cache
 * con TTL solo de aciertos (un 404 no se cachea: el perfil puede crearse justo despues del registro).
 * Un fallo de red/timeout/5xx emite {@link CoreUnavailableException}; un 404 emite {@code Optional.empty()}.
 */
@Slf4j
@Component
public class CoreProfileClient {

    private static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";
    private static final int MAX_CACHE_ENTRIES = 10_000;

    private record CacheEntry(long proveedorId, long expiresAtNanos) {
    }

    public static class CoreUnavailableException extends RuntimeException {
        public CoreUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final WebClient webClient;
    private final String serviceKey;
    private final Duration timeout;
    private final long ttlNanos;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public CoreProfileClient(
            WebClient.Builder builder,
            @Value("${findu.core.base-url:http://findu-core:8082/findu-core}") String baseUrl,
            @Value("${findu.core.internal-service-key:findu-internal-saga-secret-key-2026}") String serviceKey,
            @Value("${findu.core.timeout-ms:1500}") long timeoutMs,
            @Value("${findu.core.cache-ttl-seconds:300}") long ttlSeconds) {
        this(builder.baseUrl(baseUrl).build(), serviceKey, Duration.ofMillis(timeoutMs),
                Duration.ofSeconds(ttlSeconds), System::nanoTime);
    }

    CoreProfileClient(WebClient webClient, String serviceKey, Duration timeout, Duration ttl, LongSupplier nanoClock) {
        this.webClient = webClient;
        this.serviceKey = serviceKey;
        this.timeout = timeout;
        this.ttlNanos = ttl.toNanos();
        this.nanoClock = nanoClock;
    }

    /** @return proveedorId del authUserId, vacio si core responde 404; error CoreUnavailableException si core no responde. */
    public Mono<Optional<Long>> resolveProveedorId(String authUserId) {
        CacheEntry cached = cache.get(authUserId);
        if (cached != null) {
            if (nanoClock.getAsLong() - cached.expiresAtNanos() < 0) {
                return Mono.just(Optional.of(cached.proveedorId()));
            }
            cache.remove(authUserId, cached);
        }
        return webClient.get()
                .uri("/internal/perfiles/by-auth/{authUserId}", authUserId)
                .header(HEADER_INTERNAL_KEY, serviceKey)
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();
                    if (status.value() == 404) {
                        return Mono.just(Optional.<Long>empty());
                    }
                    if (!status.is2xxSuccessful()) {
                        return Mono.<Optional<Long>>error(
                                new IllegalStateException("core respondio HTTP " + status.value()));
                    }
                    return response.bodyToMono(JsonNode.class).map(body -> {
                        JsonNode id = body.get("perfilId");
                        if (id == null || !id.canConvertToLong()) {
                            throw new IllegalStateException("respuesta de core sin perfilId");
                        }
                        return Optional.of(id.asLong());
                    });
                })
                .timeout(timeout)
                .doOnNext(opt -> opt.ifPresent(id -> remember(authUserId, id)))
                .onErrorMap(e -> !(e instanceof CoreUnavailableException),
                        e -> new CoreUnavailableException("core no disponible: " + e.getClass().getSimpleName(), e));
    }

    private void remember(String authUserId, long proveedorId) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(authUserId, new CacheEntry(proveedorId, nanoClock.getAsLong() + ttlNanos));
    }
}

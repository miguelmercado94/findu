package com.findu.security.application.port.output.persistence;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Store temporal del OTP de teléfono. Implementado por Redis (perfiles qa/pdn) o
 * por un fallback in-memory (dev y tests sin Redis).
 */
public interface PhoneOtpStorePort {
    Mono<Void> save(String phoneKey, String json, Duration ttl);
    Mono<String> find(String phoneKey);
    Mono<Void> delete(String phoneKey);
}

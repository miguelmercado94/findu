package com.findu.transaction.infrastructure.adapter.input.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Valida el header {@code X-Internal-Service-Key} de los endpoints /internal/settlements/**.
 * Fail-closed: si {@code findu.internal.service-key} esta vacio, nadie esta autorizado.
 * Comparacion en tiempo constante.
 */
@Component
public class InternalServiceKeyGuard {

    public static final String HEADER = "X-Internal-Service-Key";

    @Value("${findu.internal.service-key:}")
    private String expectedKey;

    public boolean isAuthorized(String headerValue) {
        if (expectedKey == null || expectedKey.isBlank() || headerValue == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                headerValue.getBytes(StandardCharsets.UTF_8));
    }
}

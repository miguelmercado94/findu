package com.findu.security.mapper;

import com.findu.security.dto.response.HealthResponse;

import java.util.Map;

/**
 * Mapeo entre modelos de dominio/servicio y DTOs para health.
 */
public final class HealthMapper {

    private HealthMapper() {
    }

    public static HealthResponse toResponse(Map<String, String> healthMap) {
        return new HealthResponse(
                healthMap.getOrDefault("status", "UNKNOWN"),
                healthMap.getOrDefault("application", "findu-spring-security")
        );
    }
}

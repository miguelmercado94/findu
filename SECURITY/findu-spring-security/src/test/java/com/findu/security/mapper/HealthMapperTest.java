package com.findu.security.mapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthMapperTest {

    @Test
    void toResponse_defaults() {
        assertThat(HealthMapper.toResponse(Map.of()).status()).isEqualTo("UNKNOWN");
    }

    @Test
    void toResponse_values() {
        var r = HealthMapper.toResponse(Map.of("status", "UP", "application", "x"));
        assertThat(r.status()).isEqualTo("UP");
        assertThat(r.application()).isEqualTo("x");
    }
}

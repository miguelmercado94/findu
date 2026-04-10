package com.findu.security.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSignerFactoryImplTest {

    @Test
    void getSigner_hs256_rs256_es256() {
        byte[] secret = "0123456789abcdef0123456789abcdef".getBytes();
        JwtSignerFactoryImpl f = new JwtSignerFactoryImpl(secret);
        assertThat(f.getSigner("HS256")).isNotNull();
        assertThat(f.getSigner("rs256")).isNotNull();
        assertThat(f.getSigner("ES256")).isNotNull();
    }

    @Test
    void getSigner_blank_throws() {
        JwtSignerFactoryImpl f = new JwtSignerFactoryImpl(new byte[32]);
        assertThatThrownBy(() -> f.getSigner(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getSigner_unknown_throws() {
        JwtSignerFactoryImpl f = new JwtSignerFactoryImpl(new byte[32]);
        assertThatThrownBy(() -> f.getSigner("XX999"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

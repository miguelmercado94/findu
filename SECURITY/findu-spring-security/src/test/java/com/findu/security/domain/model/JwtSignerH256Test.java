package com.findu.security.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSignerH256Test {

    @Test
    void signer_producesNonEmptySignature() {
        JwtSignerH256 s = new JwtSignerH256("secret-key-at-least-32-bytes-long!!".getBytes());
        String sig = s.signer("header.payload");
        assertThat(sig).isNotBlank();
    }
}

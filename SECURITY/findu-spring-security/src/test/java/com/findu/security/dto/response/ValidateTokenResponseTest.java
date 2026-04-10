package com.findu.security.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateTokenResponseTest {

    @Test
    void invalid_factory() {
        ValidateTokenResponse v = ValidateTokenResponse.invalid();
        assertThat(v.tokenValid()).isFalse();
        assertThat(v.header()).isNull();
        assertThat(v.payload()).isNull();
    }
}

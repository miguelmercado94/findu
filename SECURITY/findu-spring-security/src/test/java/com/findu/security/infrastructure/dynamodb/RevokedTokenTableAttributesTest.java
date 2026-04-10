package com.findu.security.infrastructure.dynamodb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevokedTokenTableAttributesTest {

    @Test
    void constants_nonBlank() {
        assertThat(RevokedTokenTableAttributes.ACCESS_TOKEN_HASH).isNotBlank();
        assertThat(RevokedTokenTableAttributes.GSI_REFRESH_TOKEN_HASH).isNotBlank();
    }
}

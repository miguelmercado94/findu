package com.findu.security.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashUtilsTest {

    @Test
    void sha256Hex_knownInput_is64HexChars() {
        String h = TokenHashUtils.sha256Hex("token");
        assertThat(h).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256Hex_blank_returnsEmpty() {
        assertThat(TokenHashUtils.sha256Hex(null)).isEmpty();
        assertThat(TokenHashUtils.sha256Hex("  ")).isEmpty();
    }
}

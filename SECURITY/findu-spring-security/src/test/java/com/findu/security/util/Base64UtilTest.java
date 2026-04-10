package com.findu.security.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Base64UtilTest {

    @Test
    void encode_bytes_roundTrip() {
        byte[] raw = "hello".getBytes(StandardCharsets.UTF_8);
        String enc = Base64Util.encode(raw);
        assertThat(Base64Util.decode(enc)).isEqualTo(raw);
    }

    @Test
    void encode_string_null_returnsNull() {
        assertThat(Base64Util.encode((String) null)).isNull();
    }

    @Test
    void decode_blank_returnsNull() {
        assertThat(Base64Util.decode("")).isNull();
        assertThat(Base64Util.decode("   ")).isNull();
    }

    @Test
    void decodeStandard_invalid_returnsNull() {
        assertThat(Base64Util.decodeStandard("not!!!valid")).isNull();
    }

    @Test
    void decodeToString_roundTrip() {
        String enc = Base64Util.encode("hola");
        assertThat(Base64Util.decodeToString(enc)).isEqualTo("hola");
    }
}

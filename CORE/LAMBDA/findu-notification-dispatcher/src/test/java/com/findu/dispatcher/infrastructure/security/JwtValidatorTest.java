package com.findu.dispatcher.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtValidatorTest {

    static final String UTF8_SECRET = "secret-key-at-least-32-bytes-long-for-findu-security-environment!";
    static final byte[] RAW = "findu-default-secret-key-at-least-256-bits-for-hs256".getBytes(StandardCharsets.UTF_8);
    static final String B64_SECRET = Base64.getEncoder().encodeToString(RAW);

    static String token(byte[] keyBytes, String typ, Long userId, long expMillisFromNow) {
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        var b = Jwts.builder().header().type(typ).and().subject("u")
                .expiration(new Date(System.currentTimeMillis() + expMillisFromNow))
                .signWith(key, Jwts.SIG.HS256);
        if (userId != null) {
            b.claim("userId", userId);
        }
        return b.compact();
    }

    @Test
    void valido_base64() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken(token(RAW, "JWT", 7L, 60_000))).contains("7");
    }

    @Test
    void valido_utf8() {
        JwtValidator v = new JwtValidator(UTF8_SECRET);
        assertThat(v.validateAccessToken(token(UTF8_SECRET.getBytes(StandardCharsets.UTF_8), "JWT", 7L, 60_000))).contains("7");
    }

    @Test
    void secretoBase64_noValidaTokenFirmadoConLosCaracteresCrudos() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken(token(B64_SECRET.getBytes(StandardCharsets.UTF_8), "JWT", 7L, 60_000))).isEmpty();
    }

    @Test
    void vencido() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken(token(RAW, "JWT", 7L, -60_000))).isEmpty();
    }

    @Test
    void refreshToken() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken(token(RAW, "JWTRefresh", 7L, 60_000))).isEmpty();
    }

    @Test
    void firmaInvalida() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        byte[] other = "otro-secreto-distinto-de-al-menos-256-bits-ok!".getBytes(StandardCharsets.UTF_8);
        assertThat(v.validateAccessToken(token(other, "JWT", 7L, 60_000))).isEmpty();
    }

    @Test
    void sinClaimUserId() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken(token(RAW, "JWT", null, 60_000))).isEmpty();
    }

    @Test
    void basuraYNulos() {
        JwtValidator v = new JwtValidator(B64_SECRET);
        assertThat(v.validateAccessToken("no-es-un-jwt")).isEmpty();
        assertThat(v.validateAccessToken("")).isEmpty();
        assertThat(v.validateAccessToken(null)).isEmpty();
    }
}

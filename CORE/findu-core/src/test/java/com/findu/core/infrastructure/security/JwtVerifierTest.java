package com.findu.core.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtVerifierTest {

    private static final String UTF8_SECRET = "findu-test-secret-key-at-least-256-bits-long!!";
    private static final String B64_SECRET = Base64.getEncoder()
            .encodeToString("another-test-secret-key-with-256-bits-minimum".getBytes(StandardCharsets.UTF_8));

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(JwtVerifier.decodeSecret(secret));
    }

    private static String token(String secret, String typ, Date exp, Object userId) {
        var b = Jwts.builder().header().type(typ).and()
                .subject("prov").claim("role", "ROLE_OUR_PROVEEDOR")
                .issuedAt(new Date()).expiration(exp);
        if (userId != null) {
            b.claim("userId", userId);
        }
        return b.signWith(key(secret), Jwts.SIG.HS256).compact();
    }

    private static Date in(long ms) {
        return new Date(System.currentTimeMillis() + ms);
    }

    @Test
    void tokenValidoConSecretoUtf8() {
        var p = new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWT", in(60_000), 2L));
        assertThat(p).isPresent();
        assertThat(p.get().userId()).isEqualTo(2L);
        assertThat(p.get().username()).isEqualTo("prov");
        assertThat(p.get().role()).isEqualTo("ROLE_OUR_PROVEEDOR");
    }

    @Test
    void tokenValidoConSecretoBase64() {
        assertThat(new JwtVerifier(B64_SECRET).verify(token(B64_SECRET, "JWT", in(60_000), 7))).isPresent();
    }

    @Test
    void userIdComoStringSeAcepta() {
        assertThat(new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWT", in(60_000), "9")).get().userId())
                .isEqualTo(9L);
    }

    @Test
    void expiradoSeRechaza() {
        assertThat(new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWT", in(-60_000), 2L))).isEmpty();
    }

    @Test
    void refreshTokenSeRechaza() {
        assertThat(new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWTRefresh", in(60_000), 2L))).isEmpty();
    }

    @Test
    void algNoneSeRechaza() {
        String none = Jwts.builder().subject("prov").claim("userId", 2L).expiration(in(60_000)).compact();
        assertThat(new JwtVerifier(UTF8_SECRET).verify(none)).isEmpty();
    }

    @Test
    void algHs512SeRechaza() {
        SecretKey k = Keys.hmacShaKeyFor(UTF8_SECRET.getBytes(StandardCharsets.UTF_8));
        String hs512 = Jwts.builder().subject("prov").claim("userId", 2L).expiration(in(60_000))
                .signWith(Keys.hmacShaKeyFor((UTF8_SECRET + UTF8_SECRET).getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512)
                .compact();
        assertThat(k).isNotNull();
        assertThat(new JwtVerifier(UTF8_SECRET).verify(hs512)).isEmpty();
    }

    @Test
    void firmaInvalidaSeRechaza() {
        String forged = token("otro-secreto-completamente-distinto-de-256-bits!!", "JWT", in(60_000), 2L);
        assertThat(new JwtVerifier(UTF8_SECRET).verify(forged)).isEmpty();
    }

    @Test
    void sinUserIdSeRechaza() {
        assertThat(new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWT", in(60_000), null))).isEmpty();
    }

    @Test
    void sinExpiracionSeRechaza() {
        assertThat(new JwtVerifier(UTF8_SECRET).verify(token(UTF8_SECRET, "JWT", null, 2L))).isEmpty();
    }

    @Test
    void basuraYNulosSeRechazan() {
        JwtVerifier v = new JwtVerifier(UTF8_SECRET);
        assertThat(v.verify("no.es.jwt")).isEmpty();
        assertThat(v.verify("")).isEmpty();
        assertThat(v.verify(null)).isEmpty();
    }

    @Test
    void sinSecretoNingunTokenEsValido() {
        JwtVerifier v = new JwtVerifier("");
        assertThat(v.isConfigured()).isFalse();
        assertThat(v.verify(token(UTF8_SECRET, "JWT", in(60_000), 2L))).isEmpty();
    }
}

package com.findu.security.application.service.impl;

import com.findu.security.domain.model.Jwt;
import com.findu.security.domain.model.JwtHeader;
import com.findu.security.domain.model.JwtPayload;
import com.findu.security.util.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @Test
    void generate_parse_roundTrip() {
        JwtPayload p = new JwtPayload();
        p.setSub("user1");
        p.setIat(Instant.now().getEpochSecond());
        p.setExp(Instant.now().getEpochSecond() + 3600);
        JwtHeader h = new JwtHeader();
        h.setAlg("HS256");
        h.setTyp(SecurityConstants.JWT_HEADER_TYP_ACCESS);
        Jwt jwt = new Jwt();
        jwt.setHeader(h);
        jwt.setPayload(p);

        String compact = jwtService.generateToken(jwt);
        assertThat(jwtService.isValid(compact)).isTrue();
        assertThat(jwtService.parse(compact).getSub()).isEqualTo("user1");
        assertThat(jwtService.isAccessToken(compact)).isTrue();
    }

    @Test
    void isValid_blank_false() {
        assertThat(jwtService.isValid(null)).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void getHeader_malformed_returnsEmptyMap() {
        assertThat(jwtService.getHeader("bad")).isEmpty();
    }

    @Test
    void isRefreshToken_detectsTyp() {
        JwtPayload p = new JwtPayload();
        p.setSub("u");
        p.setIat(Instant.now().getEpochSecond());
        p.setExp(Instant.now().getEpochSecond() + 3600);
        JwtHeader h = new JwtHeader();
        h.setAlg("HS256");
        h.setTyp(SecurityConstants.JWT_HEADER_TYP_REFRESH);
        Jwt jwt = new Jwt();
        jwt.setHeader(h);
        jwt.setPayload(p);
        String t = jwtService.generateToken(jwt);
        assertThat(jwtService.isRefreshToken(t)).isTrue();
        assertThat(jwtService.isAccessToken(t)).isFalse();
    }

    @Test
    void getExpiration_matchesPayloadExp() {
        long exp = Instant.now().getEpochSecond() + 7200;
        JwtPayload p = new JwtPayload();
        p.setSub("u");
        p.setIat(Instant.now().getEpochSecond());
        p.setExp(exp);
        JwtHeader h = new JwtHeader();
        h.setAlg("HS256");
        h.setTyp(SecurityConstants.JWT_HEADER_TYP_ACCESS);
        Jwt jwt = new Jwt();
        jwt.setHeader(h);
        jwt.setPayload(p);

        String compact = jwtService.generateToken(jwt);
        assertThat(jwtService.getExpiration(compact).getEpochSecond()).isEqualTo(exp);
    }

    @Test
    void generateToken_withSigner_usesHeaderPayloadMaps() {
        Jwt jwt = new Jwt();
        jwt.setHeader(null);
        JwtPayload pl = new JwtPayload();
        pl.setSub("sub");
        pl.setIat(Instant.now().getEpochSecond());
        pl.setExp(Instant.now().getEpochSecond() + 120);
        pl.setIss("iss");
        pl.setAud("aud");
        jwt.setPayload(pl);
        jwt.setSigner(data -> "customsig");

        String compact = jwtService.generateToken(jwt);
        assertThat(compact).endsWith(".customsig");
        assertThat(compact.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_jjwt_extraClaims() {
        JwtPayload p = new JwtPayload();
        p.setSub("u");
        p.setIat(Instant.now().getEpochSecond());
        p.setExp(Instant.now().getEpochSecond() + 3600);
        p.setIss("findu");
        p.setAud("api");
        Map<String, Object> extra = new HashMap<>();
        extra.put("k", "v");
        p.setClaims(extra);
        JwtHeader h = new JwtHeader();
        h.setAlg("HS256");
        h.setTyp(SecurityConstants.JWT_HEADER_TYP_ACCESS);
        Jwt jwt = new Jwt();
        jwt.setHeader(h);
        jwt.setPayload(p);

        String compact = jwtService.generateToken(jwt);
        assertThat(jwtService.parse(compact).getClaims()).containsKey("k");
    }
}

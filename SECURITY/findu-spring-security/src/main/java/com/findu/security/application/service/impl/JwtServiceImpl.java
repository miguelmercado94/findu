package com.findu.security.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findu.security.application.service.JwtService;
import com.findu.security.domain.model.Jwt;
import com.findu.security.domain.model.JwtClaims;
import com.findu.security.domain.model.JwtHeader;
import com.findu.security.domain.model.JwtPayload;
import com.findu.security.domain.model.JwtSigner;
import com.findu.security.util.Base64Util;
import com.findu.security.util.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación de JwtService usando JJWT.
 * Genera y valida tokens con HS256 usando el secret configurado.
 */
@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;

    public JwtServiceImpl(@Value("${jwt.secret:findu-default-secret-key-at-least-256-bits-for-hs256}") String secret) {
        byte[] keyBytes = decodeSecret(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Decodifica jwt.secret desde Base64 estándar si viene codificado; si no, usa el string en UTF-8. */
    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return new byte[0];
        }
        byte[] decoded = Base64Util.decodeStandard(secret);
        if (decoded != null && decoded.length > 0) {
            return decoded;
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String generateToken(Jwt jwt) {
        JwtSigner signer = jwt.getSigner();
        if (signer != null) {
            return generateTokenWithSigner(jwt, signer);
        }
        return generateTokenWithJjwt(jwt);
    }

    /**
     * Genera el token usando el JwtSigner del modelo (header.payload.signature).
     */
    private String generateTokenWithSigner(Jwt jwt, JwtSigner signer) {
        try {
            String headerJson = JSON.writeValueAsString(headerToMap(jwt.getHeader()));
            String payloadJson = JSON.writeValueAsString(payloadToMap(jwt.getPayload()));
            String headerB64 = Base64Util.encode(headerJson);
            String payloadB64 = Base64Util.encode(payloadJson);
            String data = headerB64 + "." + payloadB64;
            String signature = signer.signer(data);
            return data + "." + signature;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando JWT", e);
        }
    }

    private static Map<String, Object> headerToMap(JwtHeader h) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (h == null) {
            m.put("alg", "HS256");
            m.put("typ", "JWT");
            return m;
        }
        if (h.getAlg() != null) m.put("alg", h.getAlg());
        if (h.getTyp() != null) m.put("typ", h.getTyp());
        if (h.getKid() != null) m.put("kid", h.getKid());
        if (h.getCustomClaims() != null) m.putAll(h.getCustomClaims());
        return m;
    }

    private static Map<String, Object> payloadToMap(JwtPayload p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p == null) return m;
        if (p.getSub() != null) m.put("sub", p.getSub());
        if (p.getIat() != null) m.put("iat", p.getIat());
        if (p.getExp() != null) m.put("exp", p.getExp());
        if (p.getIss() != null) m.put("iss", p.getIss());
        if (p.getAud() != null) m.put("aud", p.getAud());
        if (p.getClaims() != null) m.putAll(p.getClaims());
        return m;
    }

    /**
     * Fallback cuando el Jwt no trae signer (usa JJWT con la clave inyectada).
     */
    private String generateTokenWithJjwt(Jwt jwt) {
        JwtPayload p = jwt.getPayload();
        JwtHeader h = jwt.getHeader();
        var builder = Jwts.builder()
                .subject(p.getSub())
                .issuedAt(p.getIat() != null ? Date.from(Instant.ofEpochSecond(p.getIat())) : new Date())
                .expiration(p.getExp() != null ? Date.from(Instant.ofEpochSecond(p.getExp())) : new Date(System.currentTimeMillis() + 86400_000))
                .signWith(signingKey);
        if (p.getIss() != null) builder.issuer(p.getIss());
        if (p.getAud() != null) builder.audience().add(p.getAud());
        if (p.getClaims() != null && !p.getClaims().isEmpty()) {
            for (Map.Entry<String, Object> e : p.getClaims().entrySet()) {
                if (!Claims.SUBJECT.equals(e.getKey()) && !Claims.ISSUED_AT.equals(e.getKey())
                        && !Claims.EXPIRATION.equals(e.getKey()) && !Claims.ISSUER.equals(e.getKey())
                        && !Claims.AUDIENCE.equals(e.getKey())) {
                    builder.claim(e.getKey(), e.getValue());
                }
            }
        }
        if (h != null && h.getAlg() != null) builder.header().add("alg", h.getAlg());
        if (h != null && h.getTyp() != null) builder.header().add("typ", h.getTyp());
        if (h != null && h.getKid() != null) builder.header().add("kid", h.getKid());
        return builder.compact();
    }

    @Override
    public boolean isValid(String jwt) {
        if (jwt == null || jwt.isBlank()) return false;
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(jwt);
            return jws.getPayload().getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JwtClaims parse(String jwt) {
        Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(jwt);
        Claims c = jws.getPayload();
        JwtClaims out = new JwtClaims();
        out.setSub(c.getSubject());
        out.setIat(c.getIssuedAt() != null ? c.getIssuedAt().toInstant().getEpochSecond() : null);
        out.setExp(c.getExpiration() != null ? c.getExpiration().toInstant().getEpochSecond() : null);
        out.setIss(c.getIssuer());
        out.setAud(c.getAudience() != null && !c.getAudience().isEmpty() ? c.getAudience().iterator().next() : null);
        Map<String, Object> custom = new HashMap<>();
        c.forEach((k, v) -> {
            if (!Claims.SUBJECT.equals(k) && !Claims.ISSUED_AT.equals(k) && !Claims.EXPIRATION.equals(k)
                    && !Claims.ISSUER.equals(k) && !Claims.AUDIENCE.equals(k)) {
                custom.put(k, v);
            }
        });
        out.setClaims(custom);
        return out;
    }

    @Override
    public Instant getExpiration(String jwt) {
        Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(jwt);
        Date exp = jws.getPayload().getExpiration();
        return exp != null ? exp.toInstant() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHeader(String jwt) {
        if (jwt == null || jwt.isBlank()) return new LinkedHashMap<>();
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) return new LinkedHashMap<>();
        String headerJson = Base64Util.decodeToString(parts[0]);
        if (headerJson == null || headerJson.isBlank()) return new LinkedHashMap<>();
        try {
            return JSON.readValue(headerJson, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    @Override
    public boolean isRefreshToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }
        Object typ = getHeader(jwt).get("typ");
        return typ != null && SecurityConstants.JWT_HEADER_TYP_REFRESH.equalsIgnoreCase(typ.toString().trim());
    }

    @Override
    public boolean isAccessToken(String jwt) {
        return !isRefreshToken(jwt);
    }
}

package com.findu.core.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Verifica JWT emitidos por findu-spring-security. Solo HS256; rechaza alg none/otros, typ JWTRefresh,
 * tokens vencidos, firma invalida y tokens sin claim userId. Secreto: Base64 y, si falla, UTF-8
 * (misma regla que SecurityConfig.decodeSecret de security).
 */
public class JwtVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtVerifier.class);
    private static final String HS256 = "HS256";

    private final SecretKey key;

    public JwtVerifier(String secret) {
        this.key = buildKey(secret);
    }

    /** Misma regla que security: primero Base64, si falla (o queda vacio) UTF-8 plano. */
    static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return new byte[0];
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret.trim());
            if (decoded.length > 0) {
                return decoded;
            }
        } catch (IllegalArgumentException e) {
            // no es Base64: se usa UTF-8
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private static SecretKey buildKey(String secret) {
        byte[] bytes = decodeSecret(secret);
        if (bytes.length == 0) {
            return null;
        }
        try {
            return Keys.hmacShaKeyFor(bytes);
        } catch (RuntimeException e) {
            log.warn("JWT_SECRET demasiado corto para HS256 (minimo 256 bits); la verificacion de tokens queda deshabilitada");
            return null;
        }
    }

    /** true si hay un secreto utilizable; si no, ningun token se considera valido. */
    public boolean isConfigured() {
        return key != null;
    }

    /** @param token JWT compacto (sin el prefijo "Bearer "). */
    public Optional<AuthenticatedPrincipal> verify(String token) {
        if (key == null || token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token.trim());
            if (!HS256.equals(jws.getHeader().getAlgorithm())) {
                return Optional.empty();
            }
            String typ = jws.getHeader().getType();
            if (typ != null && typ.equalsIgnoreCase("JWTRefresh")) {
                return Optional.empty();
            }
            Claims claims = jws.getPayload();
            if (claims.getExpiration() == null) {
                return Optional.empty();
            }
            Long userId = toLong(claims.get("userId"));
            if (userId == null) {
                return Optional.empty();
            }
            Object role = claims.get("role");
            return Optional.of(new AuthenticatedPrincipal(userId, claims.getSubject(),
                    role != null ? role.toString() : null));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT rechazado: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

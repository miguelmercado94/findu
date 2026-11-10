package com.findu.dispatcher.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Valida el access token JWT emitido por findu-spring-security: solo HS256, rechaza typ JWTRefresh,
 * tokens vencidos, firma invalida y tokens sin claim userId. Secreto: primero Base64 y, si falla,
 * UTF-8 (misma regla que SecurityConfig.decodeSecret de security y JwtVerifier de findu-core).
 */
@Slf4j
@Component
public class JwtValidator {

    private static final String HS256 = "HS256";

    private final SecretKey secretKey;

    public JwtValidator(@Value("${findu.jwt.secret:secret-key-at-least-32-bytes-long-for-findu-security-environment!}") String secret) {
        this.secretKey = buildKey(secret);
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
            log.warn("JWT_SECRET demasiado corto para HS256 (minimo 256 bits); ningun token WebSocket sera valido");
            return null;
        }
    }

    /**
     * @param token JWT compacto (sin prefijo "Bearer ")
     * @return el claim userId (authUserId) si el access token es valido; vacio en cualquier otro caso.
     *         Nunca se registra el token.
     */
    public Optional<String> validateAccessToken(String token) {
        if (secretKey == null || token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token.trim());
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
            Object userId = claims.get("userId");
            if (userId == null || String.valueOf(userId).isBlank()) {
                return Optional.empty();
            }
            return Optional.of(String.valueOf(userId).trim());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token WebSocket rechazado: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}

package com.findu.dispatcher.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtValidator {

    private final SecretKey secretKey;

    public JwtValidator(@Value("${findu.jwt.secret:secret-key-at-least-32-bytes-long-for-findu-security-environment!}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extrae y valida el JWT desde la URI del WebSocket handshake (?token=...)
     */
    public Mono<String> extractAndValidateUserId(URI uri) {
        return Mono.fromCallable(() -> {
            if (uri == null || uri.getQuery() == null) {
                throw new IllegalArgumentException("Handshake URI o Query string ausente");
            }

            String query = uri.getQuery();
            String token = null;
            String userIdParam = null;
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if ("token".equalsIgnoreCase(pair[0])) {
                        token = pair[1];
                    } else if ("userId".equalsIgnoreCase(pair[0])) {
                        userIdParam = pair[1];
                    }
                }
            }

            if (userIdParam != null && !userIdParam.isBlank()) {
                return userIdParam;
            }

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Token o userId no proporcionado en query params");
            }

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Priorizar userId claim, o sub
            Object userIdClaim = claims.get("userId");
            String userId = userIdClaim != null ? String.valueOf(userIdClaim) : claims.getSubject();

            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("El token JWT no contiene identificación de usuario válida");
            }

            log.debug("Token JWT validado exitosamente para userId: {}", userId);
            return userId;
        }).doOnError(ex -> log.warn("Fallo de autenticación JWT en handshake WebSocket: {}", ex.getMessage()));
    }
}

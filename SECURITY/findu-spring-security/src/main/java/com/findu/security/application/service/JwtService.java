package com.findu.security.application.service;

import com.findu.security.domain.model.Jwt;
import com.findu.security.domain.model.JwtClaims;

import java.time.Instant;
import java.util.Map;

/**
 * Servicio de aplicación para JWT: generar, validar y parsear tokens.
 */
public interface JwtService {

    /**
     * Genera el token compacto (header.payload.signature) a partir del modelo Jwt.
     */
    String generateToken(Jwt jwt);

    /**
     * Indica si el token es válido (firma y expiración).
     */
    boolean isValid(String jwt);

    /**
     * Parsea el token y devuelve los claims (sub, iat, exp, iss, aud, claims).
     */
    JwtClaims parse(String jwt);

    /**
     * Devuelve la fecha de expiración del token.
     */
    Instant getExpiration(String jwt);

    /**
     * Devuelve el header del token decodificado (sin verificar firma).
     * Formato: mapa con alg, typ, kid, etc. Si el token es inválido o no tiene 3 partes, devuelve mapa vacío.
     */
    Map<String, Object> getHeader(String jwt);

    /**
     * {@code true} si el header declara {@code typ} igual a refresh (solo para {@code /auth/refresh}).
     */
    boolean isRefreshToken(String jwt);

    /**
     * {@code true} si no es refresh (access u otro {@code typ} legado).
     */
    boolean isAccessToken(String jwt);
}

package com.findu.security.domain.model;

/**
 * Factory (Factory Method): devuelve la implementación de JwtSigner
 * según el algoritmo del header (HS256, RS256, ES256).
 */
public interface JwtSignerFactory {

    /**
     * Obtiene el signer correspondiente al algoritmo (p. ej. "HS256" → JwtSignerH256).
     *
     * @param algorithm nombre del algoritmo (alg del header)
     * @return implementación de JwtSigner para ese algoritmo
     * @throws IllegalArgumentException si el algoritmo no está soportado
     */
    JwtSigner getSigner(String algorithm);
}

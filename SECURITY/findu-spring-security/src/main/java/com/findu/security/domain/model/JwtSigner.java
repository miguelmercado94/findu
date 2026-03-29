package com.findu.security.domain.model;

/**
 * Contrato para firmar datos (p. ej. payload JWT).
 * Implementaciones: JwtSignerH256, JwtSignerR256, JwtSignerE256.
 */
@FunctionalInterface
public interface JwtSigner {

    /**
     * Firma el dato (normalmente base64url(header).base64url(payload)) y devuelve la firma en base64url.
     */
    String signer(String data);
}

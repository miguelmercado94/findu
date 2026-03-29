package com.findu.security.domain.model;

import com.findu.security.util.Base64Util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Firma JWT con RSA-SHA256 (alg RS256).
 * Requiere una clave privada RSA (p. ej. 2048 bits).
 */
public class JwtSignerR256 implements JwtSigner {

    private static final String ALG = "SHA256withRSA";

    private final PrivateKey privateKey;

    public JwtSignerR256(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Clave privada RSA requerida para RS256");
        }
        this.privateKey = privateKey;
    }

    @Override
    public String signer(String data) {
        try {
            Signature signature = Signature.getInstance(ALG);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signature.sign();
            return Base64Util.encode(signBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando JWT con RS256", e);
        }
    }
}

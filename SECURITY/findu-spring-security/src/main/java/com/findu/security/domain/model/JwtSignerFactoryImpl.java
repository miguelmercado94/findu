package com.findu.security.domain.model;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de la fábrica de signers.
 * HS256 usa el secret en bytes; RS256 y ES256 usan claves generadas en memoria (o inyectadas).
 */
public class JwtSignerFactoryImpl implements JwtSignerFactory {

    private static final String ALG_HS256 = "HS256";
    private static final String ALG_RS256 = "RS256";
    private static final String ALG_ES256 = "ES256";
    private static final int RSA_KEY_SIZE = 2048;

    private final Map<String, JwtSigner> signers;
    private final byte[] secretBytes;

    public JwtSignerFactoryImpl(byte[] secret) {
        this(secret, null, null);
    }

    /**
     * Constructor con claves opcionales para RS256 y ES256.
     * Si rsaPrivateKey o ecPrivateKey son null, se generan pares en memoria (útil para desarrollo).
     */
    public JwtSignerFactoryImpl(byte[] secret, PrivateKey rsaPrivateKey, PrivateKey ecPrivateKey) {
        byte[] secretCopy = secret != null ? secret.clone() : new byte[0];
        this.secretBytes = secretCopy;
        Map<String, JwtSigner> signerMap = new ConcurrentHashMap<String, JwtSigner>();
        signerMap.put(ALG_HS256, new JwtSignerH256(secretCopy));
        signerMap.put(ALG_RS256, new JwtSignerR256(rsaPrivateKey != null ? rsaPrivateKey : generateRsaPrivateKey()));
        signerMap.put(ALG_ES256, new JwtSignerE256(ecPrivateKey != null ? ecPrivateKey : generateEcPrivateKey()));
        this.signers = signerMap;
    }

    private static PrivateKey generateRsaPrivateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(RSA_KEY_SIZE, new SecureRandom());
            return gen.generateKeyPair().getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar clave RSA para RS256", e);
        }
    }

    private static PrivateKey generateEcPrivateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(256, new SecureRandom()); // 256 bits = P-256
            return gen.generateKeyPair().getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar clave EC para ES256", e);
        }
    }

    @Override
    public JwtSigner getSigner(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algoritmo JWT requerido");
        }
        String key = algorithm.trim().toUpperCase();
        if (!this.signers.containsKey(key)) {
            throw new IllegalArgumentException("Algoritmo JWT no soportado: " + algorithm);
        }
        return this.signers.get(key);
    }
}

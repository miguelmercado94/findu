package com.findu.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash determinista (SHA-256) para tokens de un solo uso que requieren búsqueda por hash
 * (p. ej. el enlace mágico de verificación de correo). Se persiste solo el hash; el valor
 * en claro viaja en el enlace y nunca se almacena.
 */
public final class TokenHasher {

    private TokenHasher() {}

    public static String sha256Hex(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value no puede ser null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

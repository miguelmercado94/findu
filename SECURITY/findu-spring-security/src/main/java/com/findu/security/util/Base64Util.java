package com.findu.security.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Codificación y decodificación Base64 URL-safe sin padding (compatible con JWT y otros usos).
 */
@UtilityClass
public class Base64Util {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Decoder STANDARD_DECODER = Base64.getDecoder();

    /**
     * Codifica bytes a Base64 URL-safe sin padding.
     */
    public static String encode(byte[] data) {
        if (data == null) return null;
        return URL_ENCODER.encodeToString(data);
    }

    /**
     * Codifica una cadena (UTF-8) a Base64 URL-safe sin padding.
     */
    public static String encode(String text) {
        if (text == null) return null;
        return URL_ENCODER.encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodifica Base64 URL-safe a bytes.
     */
    public static byte[] decode(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        return URL_DECODER.decode(base64);
    }

    /**
     * Decodifica Base64 estándar (con +, /, =) a bytes. Útil para jwt.secret en configuration.
     * Si no es Base64 válido, devuelve null (no lanza).
     */
    public static byte[] decodeStandard(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return STANDARD_DECODER.decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Decodifica Base64 URL-safe a cadena UTF-8.
     */
    public static String decodeToString(String base64) {
        byte[] decoded = decode(base64);
        return decoded != null ? new String(decoded, StandardCharsets.UTF_8) : null;
    }
}

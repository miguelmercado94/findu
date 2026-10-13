package com.findu.security.util;

import java.text.Normalizer;

/**
 * Normalización de texto para comparación insensible a tildes/mayúsculas/espacios:
 * NFD + eliminación de marcas diacríticas + minúsculas + colapso de espacios internos.
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /**
     * Normaliza una cadena para comparación. {@code null} se devuelve como "".
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String nfd = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return nfd.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Compara dos nombres de forma insensible a tildes/mayúsculas/espacios.
     */
    public static boolean equalsIgnoringAccents(String a, String b) {
        return normalize(a).equals(normalize(b));
    }
}

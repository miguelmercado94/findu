package com.findu.security.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizerTest {

    @Test
    void normalize_null_returnsEmpty() {
        assertEquals("", TextNormalizer.normalize(null));
    }

    @Test
    void normalize_stripsAccents_lowercases_collapsesWhitespace() {
        assertEquals("jose pena", TextNormalizer.normalize("  José   Peña  "));
    }

    @Test
    void equalsIgnoringAccents_matchesAcrossAccentsAndCase() {
        assertTrue(TextNormalizer.equalsIgnoringAccents("José Peña", "jose pena"));
        assertTrue(TextNormalizer.equalsIgnoringAccents("Carlos  Alberto", "carlos alberto"));
    }

    @Test
    void equalsIgnoringAccents_differentNames_false() {
        assertFalse(TextNormalizer.equalsIgnoringAccents("Carlos Mendoza", "Carlos Perez"));
    }
}

package com.findu.core.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class Secrets {

    private Secrets() {
    }

    /** Comparacion en tiempo constante; false si alguno es nulo/vacio. */
    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

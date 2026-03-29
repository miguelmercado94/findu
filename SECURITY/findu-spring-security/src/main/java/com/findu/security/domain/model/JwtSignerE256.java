package com.findu.security.domain.model;

import com.findu.security.util.Base64Util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Firma JWT con ECDSA P-256 y SHA-256 (alg ES256).
 * Requiere una clave privada EC (curva P-256).
 * Convierte la firma DER (salida de Java) a formato raw R||S que exige JWT ES256.
 */
public class JwtSignerE256 implements JwtSigner {

    private static final String ALG = "SHA256withECDSA";
    /** Tamaño en bytes de R y S para P-256. */
    private static final int P256_COORD_SIZE = 32;

    private final PrivateKey privateKey;

    public JwtSignerE256(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Clave privada EC requerida para ES256");
        }
        this.privateKey = privateKey;
    }

    @Override
    public String signer(String data) {
        try {
            Signature signature = Signature.getInstance(ALG);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] derSignature = signature.sign();
            byte[] rawSignature = derToRawConcatenation(derSignature);
            return Base64Util.encode(rawSignature);
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando JWT con ES256", e);
        }
    }

    /**
     * Convierte firma DER (SEQUENCE de dos INTEGER) a R||S con R y S de exactamente P256_COORD_SIZE bytes cada uno.
     */
    private static byte[] derToRawConcatenation(byte[] der) {
        int offset = 0;
        if (der[offset++] != 0x30) {
            throw new IllegalStateException("DER: se esperaba SEQUENCE");
        }
        int seqLen = readDerLength(der, offset);
        offset += lengthBytes(der[offset]);

        // Primer INTEGER (r)
        if (der[offset++] != 0x02) {
            throw new IllegalStateException("DER: se esperaba INTEGER r");
        }
        int rLen = readDerLength(der, offset);
        offset += lengthBytes(der[offset]);
        byte[] r = new byte[P256_COORD_SIZE];
        int rSrc = offset + (rLen > P256_COORD_SIZE ? rLen - P256_COORD_SIZE : 0);
        System.arraycopy(der, rSrc, r, 0, Math.min(rLen, P256_COORD_SIZE));
        offset += rLen;

        // Segundo INTEGER (s)
        if (der[offset++] != 0x02) {
            throw new IllegalStateException("DER: se esperaba INTEGER s");
        }
        int sLen = readDerLength(der, offset);
        offset += lengthBytes(der[offset]);
        byte[] s = new byte[P256_COORD_SIZE];
        int sSrc = offset + (sLen > P256_COORD_SIZE ? sLen - P256_COORD_SIZE : 0);
        System.arraycopy(der, sSrc, s, 0, Math.min(sLen, P256_COORD_SIZE));

        byte[] raw = new byte[P256_COORD_SIZE * 2];
        System.arraycopy(r, 0, raw, 0, P256_COORD_SIZE);
        System.arraycopy(s, 0, raw, P256_COORD_SIZE, P256_COORD_SIZE);
        return raw;
    }

    private static int readDerLength(byte[] der, int offset) {
        int first = der[offset] & 0xff;
        if (first < 0x80) {
            return first;
        }
        int numBytes = first & 0x7f;
        int len = 0;
        for (int i = 1; i <= numBytes; i++) {
            len = (len << 8) | (der[offset + i] & 0xff);
        }
        return len;
    }

    private static int lengthBytes(byte firstLenByte) {
        int first = firstLenByte & 0xff;
        if (first < 0x80) {
            return 1;
        }
        return 1 + (first & 0x7f);
    }
}

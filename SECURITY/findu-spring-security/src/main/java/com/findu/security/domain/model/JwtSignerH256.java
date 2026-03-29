package com.findu.security.domain.model;

import com.findu.security.util.Base64Util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Firma JWT con HMAC-SHA256 (alg HS256).
 */
public class JwtSignerH256 implements JwtSigner {

    private static final String ALG = "HmacSHA256";

    private final byte[] secret;

    public JwtSignerH256(byte[] secret) {
        this.secret = secret != null ? secret.clone() : new byte[0];
    }

    @Override
    public String signer(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret, ALG);
            Mac mac = Mac.getInstance(ALG);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64Util.encode(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando JWT con H256", e);
        }
    }
}

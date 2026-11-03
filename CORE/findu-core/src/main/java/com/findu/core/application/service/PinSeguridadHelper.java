package com.findu.core.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class PinSeguridadHelper {

    public static final double RADIO_TIERRA_METROS = 6_371_000.0;
    public static final double DISTANCIA_MAXIMA_INICIO_METROS = 20.0;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String activeProfile;
    private final SecretKeySpec secretKey;

    public PinSeguridadHelper(
            @Value("${spring.profiles.active:local}") String activeProfile,
            @Value("${findu.security.pin-secret:findu-pin-default-aes-key-2026-chambii}") String rawSecret
    ) {
        this.activeProfile = activeProfile != null ? activeProfile.trim().toLowerCase() : "local";
        this.secretKey = deriveKey(rawSecret);
    }

    public PinSeguridadHelper() {
        this("local", "findu-pin-default-aes-key-2026-chambii");
    }

    public boolean esAmbienteProductivoOQa() {
        if (activeProfile == null) return false;
        return activeProfile.contains("qa") || activeProfile.contains("pdn") || activeProfile.contains("prod");
    }

    public String generarPinPlano() {
        if (!esAmbienteProductivoOQa()) {
            return "1234";
        }
        int code = secureRandom.nextInt(10_000);
        return String.format("%04d", code);
    }

    public String encriptarPin(String pinPlano) {
        String valor = (pinPlano == null || pinPlano.isBlank()) ? generarPinPlano() : pinPlano.trim();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return "ENC:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Error al encriptar el PIN de la solicitud", e);
        }
    }

    public String desencriptarPin(String pinEncrypted) {
        if (pinEncrypted == null || pinEncrypted.isBlank()) {
            return !esAmbienteProductivoOQa() ? "1234" : null;
        }
        if (!pinEncrypted.startsWith("ENC:")) {
            return pinEncrypted;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(pinEncrypted.substring(4));
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return !esAmbienteProductivoOQa() ? "1234" : null;
        }
    }

    public boolean validarPin(String pinIngresado, String pinEncrypted) {
        if (pinIngresado == null || !pinIngresado.trim().matches("\\d{4}")) {
            return false;
        }
        String esperado = desencriptarPin(pinEncrypted);
        if (esperado == null) {
            return false;
        }
        return esperado.equals(pinIngresado.trim());
    }

    /**
     * Calcula la distancia en metros entre dos coordenadas GPS mediante la fórmula de Haversine.
     */
    public double calcularDistanciaMetros(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return -1.0;
        }
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double rLat1 = Math.toRadians(lat1.doubleValue());
        double rLat2 = Math.toRadians(lat2.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_METROS * c;
    }

    private SecretKeySpec deriveKey(String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest((secret != null ? secret : "findu-default").getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar la llave de cifrado de PIN", e);
        }
    }
}


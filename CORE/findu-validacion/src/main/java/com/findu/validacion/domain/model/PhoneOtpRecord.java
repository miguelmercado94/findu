package com.findu.validacion.domain.model;

import java.time.Instant;

/**
 * Registro de dominio para el control y ciclo de vida de un OTP por SMS.
 */
public record PhoneOtpRecord(
        String id,
        String phoneKey,            // e.g. "+573003763300"
        String code,                // Código OTP (o hash)
        String processCode,         // e.g. "VINCULACION_LLAVE_BREB"
        Instant expiresAt,          // Expiración
        boolean verified,           // Flag si fue validado
        Instant verifiedAt,
        Instant createdAt
) {
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}

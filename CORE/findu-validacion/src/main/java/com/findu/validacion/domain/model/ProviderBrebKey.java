package com.findu.validacion.domain.model;

import java.time.Instant;

/**
 * Entidad de dominio que representa la llave Bre-B de dispersión/consignación
 * vinculada a un proveedor de servicios.
 */
public record ProviderBrebKey(
        String id,
        Long providerId,
        String keyType,              // "CELULAR", "CEDULA", "CORREO", "ALFANUMERICO"
        String keyValue,             // e.g. "+57 300 376 3300", "1020485923"
        String bank,                 // e.g. "Bancolombia", "Nequi", "Daviplata"
        String phone,                // Teléfono con el cual se validó el OTP
        String codPhoneInternational,// "+57"
        String status,               // "ACTIVE", "INACTIVE"
        Instant createdAt,
        Instant updatedAt
) {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    public String maskedKeyValue() {
        if (keyValue == null || keyValue.length() <= 6) {
            return keyValue;
        }
        int len = keyValue.length();
        if (keyValue.startsWith("+57") && len >= 11) {
            String start = keyValue.substring(0, 7);
            String end = keyValue.substring(len - 2);
            return start + " *** **" + end;
        }
        String start = keyValue.substring(0, Math.min(3, len));
        String end = keyValue.substring(Math.max(0, len - 2));
        return start + " **** " + end;
    }
}

package com.findu.security.dto.response;

import com.findu.security.domain.model.ProviderActivation;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado de activación del proveedor. Reusado por GET /status y por los POST biometric/approve/reject.
 * {@code biometriaOk=false} con {@code motivo} indica un rechazo de negocio (no un error 4xx).
 */
public record ActivationStatusResponse(
        @Schema(example = "PENDIENTE_VALIDACIONES")
        String estadoActivacion,
        boolean otpOk,
        boolean emailOk,
        boolean biometriaOk,
        boolean soporteOk,
        @Schema(example = "https://cdn.findu.example/frontal.jpg")
        String fotoPerfilFrontalUrl,
        @Schema(description = "Motivo de rechazo de negocio de la biométrica (nombre_no_coincide|fecha_no_coincide|rostro_no_coincide); null si no aplica")
        String motivo
) {
    public static ActivationStatusResponse from(ProviderActivation a) {
        return new ActivationStatusResponse(
                a.getEstado().name(),
                a.isOtpOk(),
                a.isEmailOk(),
                a.isBiometriaOk(),
                a.isSoporteOk(),
                a.getFotoFrontalUrl(),
                null
        );
    }

    public static ActivationStatusResponse rejected(ProviderActivation a, String motivo) {
        return new ActivationStatusResponse(
                a.getEstado().name(),
                a.isOtpOk(),
                a.isEmailOk(),
                a.isBiometriaOk(),
                a.isSoporteOk(),
                a.getFotoFrontalUrl(),
                motivo
        );
    }
}

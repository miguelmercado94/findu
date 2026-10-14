package com.findu.security.dto.internal;

public record InternalActivationInitRequest(
        Long userId,
        Boolean biometriaIntentada,
        Boolean biometriaAprobada,
        Double faceMatchScore,
        String fotoFrontalUrl,
        String cedulaNombre,
        String cedulaFechaNacimiento
) {
}

package com.findu.gateway.dto;

public record BiometriaData(
        Boolean intentada,
        Boolean aprobada,
        Double faceMatchScore,
        String fotoFrontalUrl,
        String cedulaNombre,
        String cedulaFechaNacimiento
) {
}

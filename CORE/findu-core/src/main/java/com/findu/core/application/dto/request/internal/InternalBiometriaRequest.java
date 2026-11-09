package com.findu.core.application.dto.request.internal;

/**
 * Evidencia biometrica enviada por findu-spring-security. Las imagenes llegan en base64 (o data-URI)
 * y se normalizan a PNG antes de guardarse bajo biometrico/.
 */
public record InternalBiometriaRequest(
        String rostroBase64,
        String documentoFrenteBase64,
        String documentoReversoBase64,
        Double faceMatchScore
) {}

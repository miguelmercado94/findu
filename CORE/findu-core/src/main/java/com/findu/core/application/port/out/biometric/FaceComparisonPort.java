package com.findu.core.application.port.out.biometric;

/**
 * Puerto de comparacion facial entre la referencia biometrica y una foto candidata.
 * La implementacion real queda para una fase posterior.
 */
public interface FaceComparisonPort {

    FaceComparisonResult compare(byte[] reference, byte[] candidate);

    /** @param score puntaje de similitud; null si el adaptador no lo calcula */
    record FaceComparisonResult(boolean matched, Double score) {
    }
}

package com.findu.core.infrastructure.adapter.out.biometric;

import com.findu.core.application.port.out.biometric.FaceComparisonPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador provisional: siempre acepta la foto candidata.
 * Pendiente de comparacion facial real (fase posterior); reemplazar este bean por el adaptador definitivo.
 */
@Component
public class AlwaysMatchFaceComparisonAdapter implements FaceComparisonPort {

    @Override
    public FaceComparisonResult compare(byte[] reference, byte[] candidate) {
        return new FaceComparisonResult(true, null);
    }
}

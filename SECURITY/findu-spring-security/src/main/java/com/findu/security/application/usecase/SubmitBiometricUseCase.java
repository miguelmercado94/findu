package com.findu.security.application.usecase;

import com.findu.security.dto.request.BiometricSubmitRequest;
import com.findu.security.dto.response.ActivationStatusResponse;
import reactor.core.publisher.Mono;

/**
 * Procesa el envío biométrico del proveedor autenticado: re-valida nombre (sin tildes) y fecha de
 * nacimiento contra findu-core, confía el face match del cliente, fija write-once la foto frontal,
 * y recomputa el estado. Rechazos de negocio -> 200 con {@code biometriaOk=false, motivo}.
 */
public interface SubmitBiometricUseCase {
    Mono<ActivationStatusResponse> submit(BiometricSubmitRequest request);
}

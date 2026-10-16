package com.findu.security.application.port.output.externalapi;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia findu-validacion: notifica que una validación del onboarding del proveedor
 * fue completada exitosamente (PASSED) para desactivar la alerta correspondiente en vivo.
 * Operación best-effort no-fatal.
 */
public interface ValidacionResolvePort {

    /**
     * Marca una validación del onboarding del proveedor como PASSED en findu-validacion.
     *
     * @param userId         id del usuario (proveedor) en security.
     * @param validationCode código de validación (p. ej. EMAIL_VERIFICATION, BIOMETRIC_FACIAL).
     * @param nota           nota descriptiva de la resolución automática.
     * @return {@code Mono<Void>} best-effort (nunca propaga error).
     */
    Mono<Void> markPassed(Long userId, String validationCode, String nota);
}

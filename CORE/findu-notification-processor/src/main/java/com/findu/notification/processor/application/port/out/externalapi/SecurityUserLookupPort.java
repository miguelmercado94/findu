package com.findu.notification.processor.application.port.out.externalapi;

import java.util.Optional;

/**
 * Puerto de salida hacia findu-spring-security: resuelve el proveedorId de un username
 * para construir el recipient canónico (PROVEEDOR_&lt;proveedorId&gt;) del device token.
 * Degrada a {@link Optional#empty()} ante error/timeout (best-effort, no lanza).
 */
public interface SecurityUserLookupPort {

    Optional<Long> findProveedorIdByUsername(String username);
}

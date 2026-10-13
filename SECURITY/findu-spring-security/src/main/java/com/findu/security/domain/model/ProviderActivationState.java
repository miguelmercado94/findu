package com.findu.security.domain.model;

/**
 * Estado global de activación del proveedor (columna {@code estado_activacion} en {@code "user"}).
 * No sobrecarga {@code account_status} (que sigue describiendo el wizard de registro).
 */
public enum ProviderActivationState {
    /** Falta una o más de OTP/correo/biométrica (inicial). */
    PENDIENTE_VALIDACIONES,
    /** OTP+correo+biométrica completas; ticket a soporte creado; esperando aprobación manual. */
    EN_REVISION_SOPORTE,
    /** Soporte aprobó; proveedor totalmente activo. */
    ACTIVADO,
    /** Soporte rechazó al proveedor. */
    RECHAZADO
}

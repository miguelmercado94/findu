package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Agregado de activación del proveedor: estado global + los cuatro flags de validación.
 * Es una vista de dominio compuesta a partir de la fila {@code "user"} (estado + guardas de soporte)
 * y de las filas vigentes de {@code provider_activation_validation}.
 */
@Getter
@Setter
public class ProviderActivation {

    private Long userId;
    private ProviderActivationState estado = ProviderActivationState.PENDIENTE_VALIDACIONES;

    private boolean otpOk;
    private boolean emailOk;
    private boolean biometriaOk;
    private boolean soporteOk;

    /** Guarda de idempotencia: el ticket a soporte solo se dispara una vez. */
    private boolean soporteNotified;
    /** Código del ticket devuelto por help-v2 (null hasta que se crea). */
    private String soporteTicketCode;
    /** Foto frontal inmutable que pasa a ser la foto de perfil (null hasta la biométrica). */
    private String fotoFrontalUrl;

    /** True cuando las tres validaciones previas a soporte están completas. */
    public boolean validacionesPreviasCompletas() {
        return otpOk && emailOk && biometriaOk;
    }
}

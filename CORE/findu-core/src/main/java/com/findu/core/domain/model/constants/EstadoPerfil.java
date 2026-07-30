package com.findu.core.domain.model.constants;

/**
 * Estados posibles de un perfil (cliente o proveedor).
 */
public final class EstadoPerfil {

    private EstadoPerfil() {}

    /** Perfil recién creado, aún sin dirección principal. */
    public static final String INCOMPLETO = "INCOMPLETO";

    /** Perfil completo con dirección principal registrada. */
    public static final String ACTIVO = "ACTIVO";

    /** Perfil desactivado por el usuario o por reglas de negocio. */
    public static final String INACTIVO = "INACTIVO";
}

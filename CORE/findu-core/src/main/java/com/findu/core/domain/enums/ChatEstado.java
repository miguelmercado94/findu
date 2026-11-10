package com.findu.core.domain.enums;

/**
 * Estado del chat proveedor-cliente dentro de "Mis clientes".
 * Fase 3 reemplazara NO_DISPONIBLE_AUN por HABILITADO / PENDIENTE_CLIENTE.
 */
public enum ChatEstado {
    /** El cliente retiro el favorito (activo=false): el chat queda bloqueado. */
    BLOQUEADO,
    /** Vinculo activo, pero el chat aun no existe (fase 3). */
    NO_DISPONIBLE_AUN;

    public static ChatEstado desde(boolean vinculoActivo) {
        return vinculoActivo ? NO_DISPONIBLE_AUN : BLOQUEADO;
    }
}
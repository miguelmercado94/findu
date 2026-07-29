package com.findu.core.messages;

/**
 * Textos de API centralizados (errores, validaciones comunes).
 */
public final class ApiMessages {

    private ApiMessages() {
    }

    public static final class Error {
        public static final String INTERNAL = "Error interno del servidor";
        public static final String RESOURCE_NOT_FOUND = "Recurso no encontrado";
        public static final String INVALID_REQUEST = "Solicitud inválida";
        public static final String ACCESS_DENIED = "No tienes permiso para acceder a este recurso";

        private Error() {
        }
    }
}

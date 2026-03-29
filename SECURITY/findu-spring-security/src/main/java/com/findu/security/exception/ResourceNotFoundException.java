package com.findu.security.exception;

/**
 * Excepción cuando no se encuentra un recurso (ej. entidad por id).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s no encontrado con id: %s", resource, id));
    }
}

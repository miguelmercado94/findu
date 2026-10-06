package com.findu.core.domain.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s con id '%s' no encontrado", resourceName, id));
    }
}

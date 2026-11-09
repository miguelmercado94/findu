package com.findu.core.domain.exception;

/**
 * El tipo/numero de identificacion del proveedor no permite construir una ruta de almacenamiento valida.
 * Se mapea a HTTP 422.
 */
public class InvalidProviderDocumentException extends RuntimeException {
    public InvalidProviderDocumentException(String message) {
        super(message);
    }
}
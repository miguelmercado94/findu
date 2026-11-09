package com.findu.core.domain.exception;

/**
 * Fallo al operar contra el almacenamiento de objetos (S3). No incluye claves completas en el mensaje.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
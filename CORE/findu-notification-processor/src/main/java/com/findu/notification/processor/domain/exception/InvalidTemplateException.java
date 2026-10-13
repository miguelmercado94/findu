package com.findu.notification.processor.domain.exception;

/**
 * Se lanza cuando una plantilla es inválida (campos requeridos faltantes,
 * variables no declaradas, metadata incoherente con el tipo, etc.).
 */
public class InvalidTemplateException extends RuntimeException {
    public InvalidTemplateException(String message) {
        super(message);
    }
}

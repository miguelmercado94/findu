package com.findu.notification.processor.domain.exception;

/**
 * Se lanza cuando no existe una plantilla activa para el código/tipo/idioma solicitado.
 */
public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(String message) {
        super(message);
    }
}

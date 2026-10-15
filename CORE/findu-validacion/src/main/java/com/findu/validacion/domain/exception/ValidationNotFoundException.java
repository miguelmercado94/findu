package com.findu.validacion.domain.exception;

public class ValidationNotFoundException extends RuntimeException {
    public ValidationNotFoundException(String message) {
        super(message);
    }
}

package com.findu.validacion.domain.exception;

public class ActiveValidationFailedException extends RuntimeException {
    public ActiveValidationFailedException(String message) {
        super(message);
    }
}

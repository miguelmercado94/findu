package com.findu.validacion.domain.exception;

public class EndpointNotMonitoredException extends RuntimeException {
    public EndpointNotMonitoredException(String message) {
        super(message);
    }
}

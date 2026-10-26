package com.findu.validacion.infrastructure.adapter.in.web.exception;

import com.findu.validacion.domain.exception.ActiveValidationFailedException;
import com.findu.validacion.domain.exception.EndpointNotMonitoredException;
import com.findu.validacion.domain.exception.ValidationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EndpointNotMonitoredException.class)
    public ResponseEntity<ErrorResponse> handleEndpointNotMonitored(
            EndpointNotMonitoredException ex, ServerHttpRequest request) {
        log.warn("Endpoint not monitored: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ValidationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleValidationNotFound(
            ValidationNotFoundException ex, ServerHttpRequest request) {
        log.warn("Validation not found: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(com.findu.validacion.domain.exception.BrebKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBrebKeyNotFound(
            com.findu.validacion.domain.exception.BrebKeyNotFoundException ex, ServerHttpRequest request) {
        log.warn("Bre-B key not found: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(com.findu.validacion.domain.exception.InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(
            com.findu.validacion.domain.exception.InvalidOtpException ex, ServerHttpRequest request) {
        log.warn("Invalid OTP: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ActiveValidationFailedException.class)
    public ResponseEntity<ErrorResponse> handleActiveValidationFailed(
            ActiveValidationFailedException ex, ServerHttpRequest request) {
        log.warn("Active validation failed: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            WebExchangeBindException ex, ServerHttpRequest request) {
        log.warn("Validation error on request: {}", ex.getMessage());
        String details = ex.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                details,
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, ServerHttpRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, ServerHttpRequest request) {
        log.error("Internal server error at {}: ", request.getPath().value(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Unexpected internal error",
                request.getPath().value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

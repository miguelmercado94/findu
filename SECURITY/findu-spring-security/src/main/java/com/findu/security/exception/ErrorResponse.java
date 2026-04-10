package com.findu.security.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.findu.security.messages.ApiMessages;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.List;

/**
 * Estructura estándar de respuesta de error para la API.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        /** Ruta solicitada (valor de {@link org.springframework.http.server.reactive.ServerHttpRequest#getPath()}). */
        String path,
        String method,
        String backendMessage,
        List<String> details
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null, null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null, null, details);
    }

    /**
     * 403: mensaje orientado al usuario; opcionalmente con contexto de {@link Authentication}.
     */
    public static ErrorResponse accessDenied(Authentication auth, ServerWebExchange exchange, AccessDeniedException ex) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "";
        String userMessage = ApiMessages.Security.accessDeniedUserMessage(auth, method, path);
        String backend = ex != null && ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : null;
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                userMessage,
                path,
                method.isBlank() ? null : method,
                backend,
                null
        );
    }
}

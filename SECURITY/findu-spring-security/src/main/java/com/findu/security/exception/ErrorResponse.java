package com.findu.security.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
        /** Ruta o URL solicitada (en 403 de permisos suele ir la URI completa). */
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
     * Cuando solo se tiene {@link Throwable}; delega solo si es {@link AccessDeniedException}.
     */
    public static ErrorResponse accessDenied(Throwable ex, ServerWebExchange exchange) {
        if (ex instanceof AccessDeniedException ade) {
            return buildAccessDenied(ade, exchange);
        }
        throw new IllegalArgumentException("Se esperaba AccessDeniedException, recibido: " + ex.getClass().getName());
    }

    /**
     * Respuesta 403 por falta de permisos (equivalente a ApiError en MVC servlet).
     */
    public static ErrorResponse accessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return buildAccessDenied(ex, exchange);
    }

    private static ErrorResponse buildAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        String userMessage = "Acceso denegado. No tienes los permisos necesarios para acceder a este recurso. "
                + "Por favor, contacta al administrador si crees que esto es un error.";
        String uri = exchange.getRequest().getURI().toString();
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : null;
        String backend = ex.getLocalizedMessage();
        if (backend != null && backend.isBlank()) {
            backend = null;
        }
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                userMessage,
                uri,
                method,
                backend,
                null
        );
    }
}

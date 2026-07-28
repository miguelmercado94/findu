package com.findu.security.exception;

import com.findu.security.messages.ApiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para WebFlux. Devuelve ErrorResponse en JSON.
 * Intercepta excepciones lanzadas en controllers/services y las mapea a HTTP status apropiados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 401 Unauthorized ─────────────────────────────────────────────────────

    /**
     * Credenciales inválidas (login con password incorrecto).
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ErrorResponse> handleBadCredentials(BadCredentialsException ex, ServerWebExchange exchange) {
        log.debug("401 BadCredentials path={}", exchange.getRequest().getPath().value());
        return Mono.just(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Credenciales inválidas",
                exchange.getRequest().getPath().value()
        ));
    }

    /**
     * Cualquier error de autenticación no cubierto por BadCredentials
     * (token expirado, usuario no encontrado, cuenta deshabilitada, etc.)
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ErrorResponse> handleAuthenticationException(AuthenticationException ex, ServerWebExchange exchange) {
        log.debug("401 AuthenticationException path={} message={}", exchange.getRequest().getPath().value(), ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : "No autenticado";
        return Mono.just(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        ));
    }

    // ─── 403 Forbidden ────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ErrorResponse> handleSecurityAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ErrorResponse.accessDenied(ctx.getAuthentication(), exchange, ex))
                .switchIfEmpty(Mono.fromCallable(() -> ErrorResponse.accessDenied(null, exchange, ex)));
    }

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return Mono.just(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        ));
    }

    // ─── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleBadRequest(IllegalArgumentException ex, ServerWebExchange exchange) {
        return Mono.just(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        ));
    }

    /**
     * Errores de validación de @Valid (campos requeridos, formato inválido, etc.)
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        String message = details.isEmpty() ? "Datos de entrada inválidos" : details.get(0);
        return Mono.just(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                details
        ));
    }

    // ─── 4xx/5xx ResponseStatusException (lanzadas con @ResponseStatus o Mono.error) ──

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        exchange.getResponse().setStatusCode(status);
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return Mono.just(ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        ));
    }

    // ─── 409 Conflict (duplicados) ────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ErrorResponse> handleConflict(IllegalStateException ex, ServerWebExchange exchange) {
        return Mono.just(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        ));
    }

    // ─── 500 Internal Server Error (catch-all) ────────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleUnhandledException(Exception ex, ServerWebExchange exchange) {
        log.error("Error no controlado path={} : {}", exchange.getRequest().getPath().value(), ex.toString(), ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : ApiMessages.Error.INTERNAL;
        return Mono.just(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        ));
    }
}

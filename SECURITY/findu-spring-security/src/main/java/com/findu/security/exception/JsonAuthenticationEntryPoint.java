package com.findu.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public JsonAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String message = ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "No autenticado o token inválido";
        return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, message);
    }
}


package com.findu.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public JsonAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        String message = denied != null && denied.getMessage() != null && !denied.getMessage().isBlank()
                ? denied.getMessage()
                : "Acceso denegado";
        return errorResponseWriter.write(exchange, HttpStatus.FORBIDDEN, message);
    }
}


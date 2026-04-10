package com.findu.security.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private final SecurityErrorResponseWriter errorResponseWriter;

    public JsonAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "?";
        log.debug("403 AccessDenied path={} method={} exMessage={}", path, method,
                denied != null ? denied.getMessage() : null);

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    log.debug("403 context: authenticated principal={}",
                            ctx.getAuthentication() != null ? ctx.getAuthentication().getName() : null);
                    return errorResponseWriter.writeErrorResponse(exchange,
                            ErrorResponse.accessDenied(ctx.getAuthentication(), exchange, denied));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.trace("403 sin SecurityContext reactivo; mensaje genérico");
                    return errorResponseWriter.writeErrorResponse(exchange,
                            ErrorResponse.accessDenied(null, exchange, denied));
                }));
    }
}

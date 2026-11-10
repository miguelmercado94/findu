package com.findu.dispatcher.infrastructure.websocket;

import com.findu.dispatcher.infrastructure.security.WebSocketAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * En modo ENFORCE rechaza el handshake con 401/403 ANTES del upgrade (y por tanto antes de registrar
 * la sesion). En OFF/LOG no interviene: la decision y el log WS_AUTH_VIOLATION los hace el handler.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class WebSocketHandshakeAuthFilter implements WebFilter {

    private final WebSocketAuthService authService;

    @Value("${findu.websocket.endpoint:/ws/events}")
    private String websocketEndpoint;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (authService.mode() != WebSocketAuthService.Mode.ENFORCE
                || !websocketEndpoint.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        return authService.authorize(exchange.getRequest().getURI())
                .flatMap(decision -> {
                    if (decision.accepted()) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(
                            decision.httpStatus() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                });
    }
}

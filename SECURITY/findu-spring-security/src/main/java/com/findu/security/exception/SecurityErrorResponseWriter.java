package com.findu.security.exception;

import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        String path = exchange.getRequest().getPath().value();
        ErrorResponse payload = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(payload);
        } catch (Exception e) {
            String fallback = "{\"status\":" + status.value()
                    + ",\"error\":\"" + status.getReasonPhrase()
                    + "\",\"message\":\"" + escapeJson(message)
                    + "\",\"path\":\"" + escapeJson(path) + "\"}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Escribe un cuerpo JSON completo (p. ej. 403 con {@code method} y {@code backendMessage}).
     */
    public Mono<Void> writeErrorResponse(ServerWebExchange exchange, ErrorResponse body) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            String fallback = "{\"status\":" + body.status()
                    + ",\"error\":\"" + escapeJson(body.error())
                    + "\",\"message\":\"" + escapeJson(body.message())
                    + "\",\"path\":\"" + escapeJson(body.path()) + "\"}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }
        HttpStatusCode code = HttpStatus.resolve(body.status());
        exchange.getResponse().setStatusCode(code != null ? code : HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


package com.findu.security.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAccessDeniedHandlerTest {

    private JsonAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonAccessDeniedHandler(new SecurityErrorResponseWriter(new ObjectMapper()));
    }

    @Test
    void handle_writes403() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x").build());
        StepVerifier.create(handler.handle(exchange, new AccessDeniedException("no")))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}

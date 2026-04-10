package com.findu.security.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAuthenticationEntryPointTest {

    private JsonAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        entryPoint = new JsonAuthenticationEntryPoint(
                new SecurityErrorResponseWriter(new tools.jackson.databind.ObjectMapper()));
    }

    @Test
    void commence_writes401() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x").build());
        StepVerifier.create(entryPoint.commence(exchange, new BadCredentialsException("x")))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

package com.findu.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private SecurityErrorResponseWriter writer;

    @BeforeEach
    void setUp() {
        writer = new SecurityErrorResponseWriter(new ObjectMapper());
    }

    @Test
    void write_setsStatusAndJsonBody() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/p").build());
        StepVerifier.create(writer.write(exchange, HttpStatus.UNAUTHORIZED, "go away"))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void writeErrorResponse_serializesRecord() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/p").build());
        ErrorResponse body = ErrorResponse.of(403, "Forbidden", "no", "/p");
        StepVerifier.create(writer.writeErrorResponse(exchange, body))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
    }
}

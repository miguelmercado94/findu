package com.findu.security.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound() {
        var ex = new ResourceNotFoundException("missing");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/r").build());

        StepVerifier.create(handler.handleResourceNotFound(ex, exchange))
                .expectNextMatches(er -> er.status() == HttpStatus.NOT_FOUND.value() && er.message().contains("missing"))
                .verifyComplete();
    }

    @Test
    void handleBadRequest() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/r").build());

        StepVerifier.create(handler.handleBadRequest(new IllegalArgumentException("bad"), exchange))
                .expectNextMatches(er -> er.status() == HttpStatus.BAD_REQUEST.value())
                .verifyComplete();
    }

    @Test
    void handleUnhandledException_blankMessage_usesInternal() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/p").build());
        StepVerifier.create(handler.handleUnhandledException(new RuntimeException(" "), exchange))
                .expectNextMatches(er -> er.status() == HttpStatus.INTERNAL_SERVER_ERROR.value())
                .verifyComplete();
    }

    @Test
    void handleSecurityAccessDenied() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/p").build());
        StepVerifier.create(handler.handleSecurityAccessDenied(new AccessDeniedException("x"), exchange))
                .expectNextMatches(er -> er.status() == HttpStatus.FORBIDDEN.value())
                .verifyComplete();
    }
}

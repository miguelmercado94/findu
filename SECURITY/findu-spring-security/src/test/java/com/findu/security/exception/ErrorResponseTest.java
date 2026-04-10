package com.findu.security.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void accessDenied_withUser_includesMessageParts() {
        var ex = new org.springframework.security.access.AccessDeniedException("Access Denied");
        var auth = new UsernamePasswordAuthenticationToken("u", null, List.of(new SimpleGrantedAuthority("ROLE_X")));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x").build());

        ErrorResponse r = ErrorResponse.accessDenied(auth, exchange, ex);

        assertThat(r.status()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(r.message()).contains("u");
        assertThat(r.path()).isEqualTo("/api/x");
    }

    @Test
    void of_basic() {
        ErrorResponse r = ErrorResponse.of(400, "Bad Request", "msg", "/p");
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.message()).isEqualTo("msg");
    }
}

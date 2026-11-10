package com.findu.dispatcher.infrastructure.websocket;

import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import com.findu.dispatcher.infrastructure.security.CoreProfileClient;
import com.findu.dispatcher.infrastructure.security.JwtValidator;
import com.findu.dispatcher.infrastructure.security.WebSocketAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Handshake con y sin token en cada modo (el filtro solo actua en ENFORCE; OFF/LOG dejan pasar). */
class WebSocketHandshakeAuthFilterTest {

    private final AtomicBoolean passed = new AtomicBoolean();
    private final WebFilterChain chain = ex -> {
        passed.set(true);
        return Mono.empty();
    };

    private MockServerWebExchange run(String mode, String query, String path) {
        JwtValidator jwt = mock(JwtValidator.class);
        CoreProfileClient core = mock(CoreProfileClient.class);
        when(jwt.validateAccessToken("owner")).thenReturn(Optional.of("10"));
        when(core.resolveProveedorId("10")).thenReturn(Mono.just(Optional.of(42L)));
        WebSocketHandshakeAuthFilter filter = new WebSocketHandshakeAuthFilter(
                new WebSocketAuthService(jwt, core, new RecipientResolver(), mode));
        ReflectionTestUtils.setField(filter, "websocketEndpoint", "/ws/events");
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get(path + query).build());
        passed.set(false);
        filter.filter(ex, chain).block();
        return ex;
    }

    @Test
    void enforce_sinToken401SinPasarAlHandler() {
        MockServerWebExchange ex = run("ENFORCE", "?userId=PROVEEDOR_42", "/ws/events");
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(passed).isFalse();
    }

    @Test
    void enforce_canalAjeno403() {
        MockServerWebExchange ex = run("ENFORCE", "?userId=PROVEEDOR_99&token=owner", "/ws/events");
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(passed).isFalse();
    }

    @Test
    void enforce_conTokenYCanalPropioPasa() {
        MockServerWebExchange ex = run("ENFORCE", "?userId=PROVEEDOR_42&token=owner", "/ws/events");
        assertThat(ex.getResponse().getStatusCode()).isNull();
        assertThat(passed).isTrue();
    }

    @Test
    void log_sinTokenPasa() {
        run("LOG", "?userId=PROVEEDOR_42", "/ws/events");
        assertThat(passed).isTrue();
    }

    @Test
    void off_sinTokenPasa() {
        run("OFF", "?userId=PROVEEDOR_42", "/ws/events");
        assertThat(passed).isTrue();
    }

    @Test
    void enforce_otrasRutasNoSeTocan() {
        run("ENFORCE", "", "/actuator/health");
        assertThat(passed).isTrue();
    }
}

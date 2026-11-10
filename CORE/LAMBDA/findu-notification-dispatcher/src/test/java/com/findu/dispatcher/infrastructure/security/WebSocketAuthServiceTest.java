package com.findu.dispatcher.infrastructure.security;

import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import com.findu.dispatcher.infrastructure.security.WebSocketAuthService.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebSocketAuthServiceTest {

    private JwtValidator jwt;
    private CoreProfileClient core;
    private final RecipientResolver resolver = new RecipientResolver();

    @BeforeEach
    void setUp() {
        jwt = mock(JwtValidator.class);
        core = mock(CoreProfileClient.class);
        // owner: authUserId 10 -> proveedor 42 ; intruso: authUserId 11 -> proveedor 43
        when(jwt.validateAccessToken("owner")).thenReturn(Optional.of("10"));
        when(jwt.validateAccessToken("other")).thenReturn(Optional.of("11"));
        when(jwt.validateAccessToken("bad")).thenReturn(Optional.empty());
        when(core.resolveProveedorId("10")).thenReturn(Mono.just(Optional.of(42L)));
        when(core.resolveProveedorId("11")).thenReturn(Mono.just(Optional.of(43L)));
    }

    private WebSocketAuthService svc(String mode) {
        return new WebSocketAuthService(jwt, core, resolver, mode);
    }

    private static URI uri(String query) {
        return URI.create("ws://h:9000/ws/events" + (query.isEmpty() ? "" : "?" + query));
    }

    // ---- OFF ----

    @Test
    void off_aceptaUserIdSinTokenYNoTocaNada() {
        Decision d = svc("OFF").authorize(uri("userId=PROVEEDOR_42")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
        verifyNoInteractions(core);
    }

    @Test
    void off_sinNadaSeRechaza() {
        assertThat(svc("OFF").authorize(uri("")).block().accepted()).isFalse();
    }

    @Test
    void modoInvalidoOVacioEsOff() {
        assertThat(svc("raro").mode()).isEqualTo(WebSocketAuthService.Mode.OFF);
        assertThat(svc("").mode()).isEqualTo(WebSocketAuthService.Mode.OFF);
        assertThat(svc("enforce").mode()).isEqualTo(WebSocketAuthService.Mode.ENFORCE);
    }

    // ---- ENFORCE ----

    @Test
    void enforce_duenioConCanalPropioConecta() {
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42&token=owner")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void enforce_canalNumericoSeNormalizaYCoincide() {
        assertThat(svc("ENFORCE").authorize(uri("userId=42&token=owner")).block().channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void enforce_identidadSaleDelTokenSiNoHayUserId() {
        Decision d = svc("ENFORCE").authorize(uri("token=owner")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void enforce_canalAjenoRechazado403() {
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42&token=other")).block();
        assertThat(d.accepted()).isFalse();
        assertThat(d.httpStatus()).isEqualTo(403);
        assertThat(d.reason()).isEqualTo("CHANNEL_MISMATCH");
    }

    @Test
    void enforce_sinTokenRechazado401() {
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42")).block();
        assertThat(d.accepted()).isFalse();
        assertThat(d.httpStatus()).isEqualTo(401);
        assertThat(d.reason()).isEqualTo("NO_TOKEN");
        verifyNoInteractions(core);
    }

    @Test
    void enforce_tokenInvalidoRechazado401() {
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42&token=bad")).block();
        assertThat(d.accepted()).isFalse();
        assertThat(d.httpStatus()).isEqualTo(401);
        assertThat(d.reason()).isEqualTo("BAD_TOKEN");
    }

    @Test
    void enforce_coreCaidoRechaza() {
        when(core.resolveProveedorId(anyString()))
                .thenReturn(Mono.error(new CoreProfileClient.CoreUnavailableException("down", new RuntimeException())));
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42&token=owner")).block();
        assertThat(d.accepted()).isFalse();
        assertThat(d.httpStatus()).isEqualTo(403);
        assertThat(d.reason()).isEqualTo("CORE_UNAVAILABLE");
    }

    @Test
    void enforce_usuarioSinPerfilDeProveedorEsMismatch() {
        when(core.resolveProveedorId("10")).thenReturn(Mono.just(Optional.empty()));
        Decision d = svc("ENFORCE").authorize(uri("userId=PROVEEDOR_42&token=owner")).block();
        assertThat(d.accepted()).isFalse();
        assertThat(d.reason()).isEqualTo("CHANNEL_MISMATCH");
    }

    @Test
    void enforce_canalNoProveedorConTokenValidoNoConsultaCore() {
        Decision d = svc("ENFORCE").authorize(uri("userId=CLIENTE_9&token=owner")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("CLIENTE_9");
        verifyNoInteractions(core);
    }

    // ---- LOG ----

    @Test
    void log_sinTokenSigueAceptando() {
        Decision d = svc("LOG").authorize(uri("userId=PROVEEDOR_42")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void log_canalAjenoSeAcepta() {
        Decision d = svc("LOG").authorize(uri("userId=PROVEEDOR_42&token=other")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void log_tokenInvalidoSeAcepta() {
        assertThat(svc("LOG").authorize(uri("userId=PROVEEDOR_42&token=bad")).block().accepted()).isTrue();
    }

    @Test
    void log_coreCaidoSeAcepta() {
        when(core.resolveProveedorId(anyString()))
                .thenReturn(Mono.error(new CoreProfileClient.CoreUnavailableException("down", new RuntimeException())));
        Decision d = svc("LOG").authorize(uri("userId=PROVEEDOR_42&token=owner")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void log_duenioConCanalPropio() {
        Decision d = svc("LOG").authorize(uri("userId=PROVEEDOR_42&token=owner")).block();
        assertThat(d.accepted()).isTrue();
        assertThat(d.channel()).isEqualTo("PROVEEDOR_42");
    }

    @Test
    void log_sinTokenNiUserIdNoSePuedeIdentificar() {
        assertThat(svc("LOG").authorize(uri("")).block().accepted()).isFalse();
    }
}

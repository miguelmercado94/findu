package com.findu.dispatcher.infrastructure.websocket;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import com.findu.dispatcher.infrastructure.security.WebSocketAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventWebSocketHandlerTest {

    private static final String USER = "PROVEEDOR_7";
    private static final String NODE = "node-test";

    private WebSocketAuthService authService;
    private SessionRegistryPort registry;
    private RedisPubSubPort redis;
    private WebSocketSession session;
    private EventWebSocketHandler handler;
    private Sinks.Many<WebSocketMessage> inbound;

    @BeforeEach
    void setUp() {
        authService = mock(WebSocketAuthService.class);
        registry = mock(SessionRegistryPort.class);
        redis = mock(RedisPubSubPort.class);
        session = mock(WebSocketSession.class);
        inbound = Sinks.many().unicast().onBackpressureBuffer();

        HandshakeInfo handshake = mock(HandshakeInfo.class);
        when(handshake.getUri()).thenReturn(URI.create("ws://localhost:9000/ws/events?userId=" + USER));
        when(session.getHandshakeInfo()).thenReturn(handshake);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        when(session.receive()).thenReturn(inbound.asFlux());
        when(session.send(any())).thenReturn(Mono.never());
        when(session.close(any(CloseStatus.class))).thenReturn(Mono.empty());
        when(authService.authorize(any())).thenReturn(Mono.just(new WebSocketAuthService.Decision(true, USER, 0, null)));
        when(redis.registerPresence(USER, NODE)).thenReturn(Mono.empty());
        when(redis.removePresence(USER, NODE)).thenReturn(Mono.empty());
        when(registry.hasLocalSession(USER)).thenReturn(false);

        handler = new EventWebSocketHandler(authService, registry, redis,
                JsonMapper.builder().findAndAddModules().build(), new RecipientResolver());
        ReflectionTestUtils.setField(handler, "pingIntervalSeconds", 30L);
        ReflectionTestUtils.setField(handler, "nodeId", NODE);
        ReflectionTestUtils.setField(handler, "maxMissedPongs", 3);
    }

    @Test
    @DisplayName("cierre del cliente termina la cadena al instante y limpia sesion y presencia")
    void clientClose_cleansUpImmediately() {
        StepVerifier.create(handler.handle(session))
                .then(() -> inbound.tryEmitComplete())
                .verifyComplete();

        verify(registry).registerSession(USER, session);
        verify(registry).unregisterSession(USER, "s1");
        verify(redis).removePresence(USER, NODE);
    }

    @Test
    @DisplayName("review #2: zombi sin trafico se cierra en el tick 3 (90 s) con GOING_AWAY y se limpia")
    void zombie_closedAtThirdTick() {
        StepVerifier.withVirtualTime(() -> handler.handle(session))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(89))
                .thenAwait(Duration.ofSeconds(1))
                .verifyComplete();

        verify(session).close(CloseStatus.GOING_AWAY);
        verify(registry).unregisterSession(USER, "s1");
        verify(redis).removePresence(USER, NODE);
    }

    @Test
    @DisplayName("trafico entrante en cada intervalo evita el cierre del watchdog")
    void inboundActivity_preventsWatchdogClose() {
        WebSocketMessage pong = mock(WebSocketMessage.class);
        StepVerifier.Step<Void> step = StepVerifier.withVirtualTime(() -> handler.handle(session))
                .expectSubscription();
        for (int i = 0; i < 9; i++) {
            step = step.thenAwait(Duration.ofSeconds(20)).then(() -> inbound.tryEmitNext(pong));
        }
        step.then(() -> inbound.tryEmitComplete()).verifyComplete();

        verify(session, never()).close(CloseStatus.GOING_AWAY);
        verify(registry).unregisterSession(USER, "s1");
    }

    @Test
    @DisplayName("presencia segura: con otra sesion local viva no se llama removePresence")
    void presence_notRemovedWhenAnotherLocalSessionAlive() {
        when(registry.hasLocalSession(USER)).thenReturn(true);

        StepVerifier.create(handler.handle(session))
                .then(() -> inbound.tryEmitComplete())
                .verifyComplete();

        verify(registry).unregisterSession(USER, "s1");
        verify(redis, never()).removePresence(any(), any());
    }

    @Test
    @DisplayName("presencia: sin sesiones locales restantes se llama removePresence; si Redis falla no propaga")
    void presence_removedAndRedisErrorIgnored() {
        when(redis.removePresence(USER, NODE)).thenReturn(Mono.error(new IllegalStateException("redis caido")));

        StepVerifier.create(handler.handle(session))
                .then(() -> inbound.tryEmitComplete())
                .verifyComplete();

        verify(redis).removePresence(USER, NODE);
    }

    @Test
    @DisplayName("review #13: error en la cadena de sesion se loguea aparte, no cierra con BAD_DATA y limpia")
    void sessionError_notTreatedAsAuthFailure() {
        StepVerifier.create(handler.handle(session))
                .then(() -> inbound.tryEmitError(new RuntimeException("conexion abortada")))
                .verifyComplete();

        verify(session, never()).close(CloseStatus.BAD_DATA);
        verify(registry).unregisterSession(USER, "s1");
        verify(redis).removePresence(USER, NODE);
    }

    @Test
    @DisplayName("fallo de autenticacion cierra con BAD_DATA y no registra sesion")
    void authFailure_closesBadData() {
        when(authService.authorize(any())).thenReturn(Mono.just(new WebSocketAuthService.Decision(false, null, 400, "NO_IDENTITY")));

        StepVerifier.create(handler.handle(session)).verifyComplete();

        verify(session).close(CloseStatus.BAD_DATA);
        verify(registry, never()).registerSession(any(), any());
    }

    @Test
    @DisplayName("review #12: init() corrige maxMissedPongs < 1 a 3 y respeta valores validos")
    void init_correctsInvalidMaxMissedPongs() {
        ReflectionTestUtils.setField(handler, "maxMissedPongs", 0);
        handler.init();
        assertThat(ReflectionTestUtils.getField(handler, "maxMissedPongs")).isEqualTo(3);

        ReflectionTestUtils.setField(handler, "maxMissedPongs", 5);
        handler.init();
        assertThat(ReflectionTestUtils.getField(handler, "maxMissedPongs")).isEqualTo(5);
    }
}

package com.findu.dispatcher.infrastructure.adapter.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalSessionRegistryAdapterTest {

    private final LocalSessionRegistryAdapter adapter = new LocalSessionRegistryAdapter();

    private WebSocketSession session(String id, boolean open) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(open);
        return s;
    }

    @Test
    @DisplayName("review #7: hasLocalSession ignora sesiones cerradas")
    void hasLocalSession_countsOnlyOpenSessions() {
        adapter.registerSession("PROVEEDOR_7", session("closed", false));
        assertThat(adapter.hasLocalSession("PROVEEDOR_7")).isFalse();

        adapter.registerSession("PROVEEDOR_7", session("open", true));
        assertThat(adapter.hasLocalSession("PROVEEDOR_7")).isTrue();

        adapter.unregisterSession("PROVEEDOR_7", "open");
        assertThat(adapter.hasLocalSession("PROVEEDOR_7")).isFalse();
        assertThat(adapter.hasLocalSession("OTRO")).isFalse();
    }

    @Test
    @DisplayName("sendToUserLocally envia solo a sesiones abiertas")
    void sendToUserLocally_onlyOpenSessions() {
        WebSocketSession closed = session("closed", false);
        WebSocketSession open = session("open", true);
        WebSocketMessage msg = mock(WebSocketMessage.class);
        when(open.textMessage("{}")).thenReturn(msg);
        when(open.send(any())).thenReturn(Mono.empty());
        adapter.registerSession("PROVEEDOR_7", closed);
        adapter.registerSession("PROVEEDOR_7", open);

        StepVerifier.create(adapter.sendToUserLocally("PROVEEDOR_7", "{}")).verifyComplete();

        verify(open).send(any());
        verify(closed, never()).send(any());
    }
}

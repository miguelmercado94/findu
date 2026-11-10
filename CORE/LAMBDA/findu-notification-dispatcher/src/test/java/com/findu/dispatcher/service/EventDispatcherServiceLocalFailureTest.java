package com.findu.dispatcher.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.findu.dispatcher.domain.model.CanalComunicacion;
import com.findu.dispatcher.domain.model.EstadoCanal;
import com.findu.dispatcher.domain.model.EventEnvelope;
import com.findu.dispatcher.domain.model.EventType;
import com.findu.dispatcher.domain.model.EventoCanal;
import com.findu.dispatcher.domain.model.TipoCanal;
import com.findu.dispatcher.domain.model.TipoEventoCanal;
import com.findu.dispatcher.domain.port.EventFallbackPort;
import com.findu.dispatcher.domain.port.RedisPubSubPort;
import com.findu.dispatcher.domain.port.SessionRegistryPort;
import com.findu.dispatcher.domain.port.input.GestionCanalUseCase;
import com.findu.dispatcher.infrastructure.routing.RecipientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Review #7: sin sesion abierta o con envio fallido al socket local, el evento cae a FCM (no se pierde). */
class EventDispatcherServiceLocalFailureTest {

    private SessionRegistryPort sessionRegistry;
    private RedisPubSubPort redisPubSubPort;
    private EventFallbackPort eventFallbackPort;
    private GestionCanalUseCase gestionCanalUseCase;
    private EventDispatcherService service;

    @BeforeEach
    void setUp() {
        sessionRegistry = mock(SessionRegistryPort.class);
        redisPubSubPort = mock(RedisPubSubPort.class);
        eventFallbackPort = mock(EventFallbackPort.class);
        gestionCanalUseCase = mock(GestionCanalUseCase.class);
        service = new EventDispatcherService(sessionRegistry, redisPubSubPort, eventFallbackPort,
                gestionCanalUseCase, JsonMapper.builder().findAndAddModules().build(), new RecipientResolver());
        CanalComunicacion canal = new CanalComunicacion("PROVEEDOR_42", TipoCanal.CANAL_1_OFERTAS, List.of());
        canal.setEstado(EstadoCanal.ACTIVO);
        when(gestionCanalUseCase.agregarEvento(eq("PROVEEDOR_42"), any(EventoCanal.class))).thenReturn(Mono.just(canal));
        when(eventFallbackPort.sendToFallbackQueue(any())).thenReturn(Mono.empty());
    }

    private EventEnvelope<Map<String, Object>> envelope() {
        return EventEnvelope.of(EventType.OFFER_RECEIVED, "PROVEEDOR_42", Map.of("ofertaId", "abc"));
    }

    @Test
    @DisplayName("envio fallido al socket local -> cola fallback")
    void failedLocalSend_fallsBackToFcm() {
        when(sessionRegistry.hasLocalSession("PROVEEDOR_42")).thenReturn(true);
        when(sessionRegistry.sendToUserLocally(eq("PROVEEDOR_42"), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("socket roto")));

        StepVerifier.create(service.dispatch("PROVEEDOR_42", envelope(), TipoEventoCanal.OFERTA)).verifyComplete();

        verify(eventFallbackPort).sendToFallbackQueue(any());
    }

    @Test
    @DisplayName("sin sesion local abierta ni presencia en el cluster -> cola fallback")
    void noOpenLocalSession_andOffline_fallsBackToFcm() {
        when(sessionRegistry.hasLocalSession("PROVEEDOR_42")).thenReturn(false);
        when(redisPubSubPort.isUserOnlineInCluster("PROVEEDOR_42")).thenReturn(Mono.just(false));

        StepVerifier.create(service.dispatch("PROVEEDOR_42", envelope(), TipoEventoCanal.OFERTA)).verifyComplete();

        verify(sessionRegistry, never()).sendToUserLocally(anyString(), anyString());
        verify(eventFallbackPort).sendToFallbackQueue(any());
    }
}

package com.findu.dispatcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test de integración del motor de enrutamiento con la red de seguridad {@link RecipientResolver}.
 * Cubre:
 * (a) entrega local cuando la sesión está bajo el canónico y el targetUserId ya es canónico (CA1);
 * (b) tolerancia a app vieja: sesión bajo el canónico + alias aprendido, despacho por el alias
 *     normaliza y entrega al socket canónico (CA5);
 * (c) invariante del camino Rabbit: dispatch(EventEnvelope<?>) deriva el canalId del canónico y un
 *     canal INACTIVO fuerza fallback FCM (hallazgo #2/#10, CA7).
 */
@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceTest {

    @Mock
    private SessionRegistryPort sessionRegistry;
    @Mock
    private RedisPubSubPort redisPubSubPort;
    @Mock
    private EventFallbackPort eventFallbackPort;
    @Mock
    private GestionCanalUseCase gestionCanalUseCase;

    @Captor
    private ArgumentCaptor<String> canalIdCaptor;

    private RecipientResolver recipientResolver;
    private EventDispatcherService service;

    @BeforeEach
    void setUp() {
        recipientResolver = new RecipientResolver();
        // findAndAddModules() registra JSR310 (igual que el ObjectMapper autoconfigurado por Spring Boot).
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        service = new EventDispatcherService(
                sessionRegistry, redisPubSubPort, eventFallbackPort,
                gestionCanalUseCase, objectMapper, recipientResolver);
    }

    private CanalComunicacion canalConEstado(EstadoCanal estado) {
        CanalComunicacion canal = new CanalComunicacion("PROVEEDOR_42", TipoCanal.CANAL_1_OFERTAS, List.of());
        canal.setEstado(estado);
        return canal;
    }

    @Test
    void entregaLocal_cuandoSesionBajoCanonicoYTargetCanonico_noUsaFallback() {
        when(gestionCanalUseCase.agregarEvento(eq("PROVEEDOR_42"), any(EventoCanal.class)))
                .thenReturn(Mono.just(canalConEstado(EstadoCanal.ACTIVO)));
        when(sessionRegistry.hasLocalSession("PROVEEDOR_42")).thenReturn(true);
        when(sessionRegistry.sendToUserLocally(eq("PROVEEDOR_42"), anyString())).thenReturn(Mono.empty());

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EventType.OFFER_RECEIVED, "PROVEEDOR_42", Map.of("ofertaId", "abc"));

        StepVerifier.create(service.dispatch("PROVEEDOR_42", envelope, TipoEventoCanal.OFERTA))
                .verifyComplete();

        verify(sessionRegistry).sendToUserLocally(eq("PROVEEDOR_42"), anyString());
        verify(eventFallbackPort, never()).sendToFallbackQueue(any());
    }

    @Test
    void appVieja_aliasAprendido_normalizaYEntregaAlSocketCanonico() {
        // La sesión del socket vive bajo el canónico PROVEEDOR_42 (proveedorId).
        recipientResolver.learnAlias("PROVEEDOR_1001", "PROVEEDOR_42"); // authUserId -> proveedorId
        when(gestionCanalUseCase.agregarEvento(eq("PROVEEDOR_1001"), any(EventoCanal.class)))
                .thenReturn(Mono.just(canalConEstado(EstadoCanal.ACTIVO)));
        when(sessionRegistry.hasLocalSession("PROVEEDOR_42")).thenReturn(true);
        when(sessionRegistry.sendToUserLocally(eq("PROVEEDOR_42"), anyString())).thenReturn(Mono.empty());

        // El productor viejo despacha contra el alias PROVEEDOR_1001 (authUserId).
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EventType.CHAT_MESSAGE, "PROVEEDOR_1001", Map.of("mensaje", "hola"));

        StepVerifier.create(service.dispatch("PROVEEDOR_1001", envelope, TipoEventoCanal.MENSAJE))
                .verifyComplete();

        // El historial del canal y la entrega usan el canónico, no el alias.
        verify(sessionRegistry).sendToUserLocally(eq("PROVEEDOR_42"), anyString());
        verify(sessionRegistry, never()).hasLocalSession("PROVEEDOR_1001");
        verify(eventFallbackPort, never()).sendToFallbackQueue(any());
    }

    @Test
    void caminoRabbit_derivaCanalIdDelCanonico_yCanalInactivoFuerzaFcm() {
        // El payload trae el par canonicalRecipient/aliasRecipient que core incluye.
        Map<String, Object> payload = Map.of(
                "canonicalRecipient", "PROVEEDOR_42",
                "aliasRecipient", "PROVEEDOR_1001",
                "mensaje", "nueva solicitud");
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EventType.SYSTEM_ALERT, "PROVEEDOR_1001", payload);

        when(gestionCanalUseCase.agregarEvento(anyString(), any(EventoCanal.class)))
                .thenReturn(Mono.just(canalConEstado(EstadoCanal.INACTIVO)));
        when(eventFallbackPort.sendToFallbackQueue(any())).thenReturn(Mono.empty());
        lenient().when(sessionRegistry.hasLocalSession(anyString())).thenReturn(true);

        StepVerifier.create(service.dispatch(envelope))
                .verifyComplete();

        // El canalId se deriva del canónico (aprendido del alias en el payload), no del eventId UUID.
        verify(gestionCanalUseCase).agregarEvento(canalIdCaptor.capture(), any(EventoCanal.class));
        assertThat(canalIdCaptor.getValue()).isEqualTo("PROVEEDOR_42");

        // Canal INACTIVO: siempre FCM, aunque haya sesión local.
        verify(eventFallbackPort).sendToFallbackQueue(any());
        verify(sessionRegistry, never()).sendToUserLocally(anyString(), anyString());
    }

    @Test
    void otrosRoles_noSeNormalizanComoProveedor() {
        when(gestionCanalUseCase.agregarEvento(eq("CLIENTE_7"), any(EventoCanal.class)))
                .thenReturn(Mono.just(new CanalComunicacion("CLIENTE_7", TipoCanal.CANAL_1_OFERTAS, List.of())));
        when(sessionRegistry.hasLocalSession("CLIENTE_7")).thenReturn(false);
        when(redisPubSubPort.isUserOnlineInCluster("CLIENTE_7")).thenReturn(Mono.just(false));
        when(eventFallbackPort.sendToFallbackQueue(any())).thenReturn(Mono.empty());

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                EventType.OFFER_RECEIVED, "CLIENTE_7", Map.of("ofertaId", "abc"));

        StepVerifier.create(service.dispatch("CLIENTE_7", envelope, TipoEventoCanal.OFERTA))
                .verifyComplete();

        verify(eventFallbackPort).sendToFallbackQueue(any());
    }
}

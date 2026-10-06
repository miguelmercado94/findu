package com.findu.dispatcher.presentation.controller;

import com.findu.dispatcher.domain.model.*;
import com.findu.dispatcher.domain.port.input.GestionCanalUseCase;
import com.findu.dispatcher.service.EventDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Endpoint de Pruebas y Simulación para validar Canales 1, 2 y 3 sin requerir otros actores aún.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/canales")
@RequiredArgsConstructor
public class ChannelTestController {

    private final GestionCanalUseCase gestionCanalUseCase;
    private final EventDispatcherService eventDispatcherService;

    public record AbrirCanalTestRequest(
            String canalId,
            TipoCanal tipoCanal,
            String clienteId,
            String proveedorOSoporteId
    ) {}

    public record SimularEventoRequest(
            String canalId,
            String targetUserId,
            String emisorId,
            TipoEventoCanal tipoEvento,
            Object contenido
    ) {}

    public record CambiarEstadoRequest(
            String canalId,
            EstadoCanal nuevoEstado
    ) {}

    @PostMapping("/abrir")
    public Mono<ResponseEntity<CanalComunicacion>> abrirCanal(@RequestBody AbrirCanalTestRequest request) {
        List<Interoperante> interoperantes = List.of(
                new Interoperante(request.clienteId(), "CLIENTE"),
                new Interoperante(request.proveedorOSoporteId() != null ? request.proveedorOSoporteId() : "BOT_SOPORTE", "INTEROPERANTE")
        );

        return gestionCanalUseCase.abrirCanal(request.canalId(), request.tipoCanal(), interoperantes)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/simular-evento")
    public Mono<ResponseEntity<String>> simularEvento(@RequestBody SimularEventoRequest request) {
        EventEnvelope<Object> envelope = EventEnvelope.of(
                EventType.CHAT_MESSAGE,
                request.targetUserId(),
                request.contenido()
        );

        return eventDispatcherService.dispatch(request.canalId(), envelope, request.tipoEvento())
                .then(Mono.just(ResponseEntity.ok("Evento inyectado y despachado correctamente al canal " + request.canalId())));
    }

    @PostMapping("/cambiar-estado")
    public Mono<ResponseEntity<CanalComunicacion>> cambiarEstado(@RequestBody CambiarEstadoRequest request) {
        return gestionCanalUseCase.cambiarEstadoCanal(request.canalId(), request.nuevoEstado())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CanalComunicacion>> verCanal(@PathVariable String id) {
        return gestionCanalUseCase.obtenerCanal(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}

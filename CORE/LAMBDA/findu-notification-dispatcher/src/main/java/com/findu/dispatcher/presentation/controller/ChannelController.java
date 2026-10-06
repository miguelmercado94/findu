package com.findu.dispatcher.presentation.controller;

import com.findu.dispatcher.domain.model.EstadoCanal;
import com.findu.dispatcher.domain.model.TipoCanal;
import com.findu.dispatcher.domain.port.input.GestionCanalUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/canales")
@RequiredArgsConstructor
public class ChannelController {

    private final GestionCanalUseCase gestionCanalUseCase;

    public record ReactivarCanalesRequest(
            String username,
            String rol // CLIENTE | PROVEEDOR
    ) {}

    public record CanalActivoResponse(
            String canalId,
            TipoCanal tipoCanal,
            EstadoCanal estado,
            int totalEventos
    ) {}

    public record CambiarEstadoProveedorRequest(
            Long proveedorId,
            boolean disponible
    ) {}

    /**
     * Endpoint invocado al hacer Login o desbloquear pantalla.
     * Reactiva todos los canales INACTIVOS del usuario y devuelve cuáles quedaron activos.
     */
    @PostMapping("/reactivar")
    public Mono<ResponseEntity<List<CanalActivoResponse>>> reactivarCanales(@RequestBody ReactivarCanalesRequest request) {
        log.info("Petición de reactivación de canales recibida. username={}, rol={}", request.username(), request.rol());
        
        return gestionCanalUseCase.reactivarCanalesUsuario(request.username(), request.rol())
                .map(canales -> canales.stream()
                        .map(c -> new CanalActivoResponse(
                                c.getId(),
                                c.getTipoCanal(),
                                c.getEstado(),
                                c.getEventos() != null ? c.getEventos().size() : 0
                        ))
                        .toList()
                )
                .map(ResponseEntity::ok);
    }

    /**
     * Endpoint invocado desde findu-core para actualizar el estado del canal del proveedor (En Línea / Fuera de Línea).
     */
    @PutMapping("/proveedor/disponibilidad")
    public Mono<ResponseEntity<Void>> actualizarDisponibilidadProveedor(@RequestBody CambiarEstadoProveedorRequest request) {
        log.info("Petición de disponibilidad de proveedor recibida: proveedorId={}, disponible={}", request.proveedorId(), request.disponible());
        EstadoCanal nuevoEstado = request.disponible() ? EstadoCanal.ACTIVO : EstadoCanal.INACTIVO;
        String canalId = "PROVEEDOR_" + request.proveedorId();

        return gestionCanalUseCase.cambiarEstadoCanal(canalId, nuevoEstado)
                .onErrorResume(e -> {
                    log.info("Canal {} no existía aún en dispatcher; abriendo con estado {}", canalId, nuevoEstado);
                    if (request.disponible()) {
                        return gestionCanalUseCase.abrirCanal(canalId, TipoCanal.CANAL_1_OFERTAS, List.of());
                    }
                    return Mono.empty();
                })
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}

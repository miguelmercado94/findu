package com.findu.dispatcher.domain.port.input;

import com.findu.dispatcher.domain.model.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GestionCanalUseCase {
    Mono<CanalComunicacion> abrirCanal(String canalId, TipoCanal tipo, List<Interoperante> interoperantes);
    Mono<CanalComunicacion> agregarEvento(String canalId, EventoCanal evento);
    Mono<CanalComunicacion> cambiarEstadoCanal(String canalId, EstadoCanal estado);
    Mono<CanalComunicacion> obtenerCanal(String canalId);
    
    /**
     * Reactiva todos los canales INACTIVOS de un usuario (al hacer login o volver a primer plano).
     */
    Mono<List<CanalComunicacion>> reactivarCanalesUsuario(String usuarioId, String rol);
}

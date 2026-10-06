package com.findu.dispatcher.domain.port.output;

import com.findu.dispatcher.domain.model.CanalComunicacion;
import com.findu.dispatcher.domain.model.EstadoCanal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelRepositoryPort {
    Mono<CanalComunicacion> guardar(CanalComunicacion canal);
    Mono<CanalComunicacion> buscarPorId(String id);
    Flux<CanalComunicacion> buscarPorUsuarioId(String usuarioId);
    Mono<CanalComunicacion> cambiarEstado(String canalId, EstadoCanal nuevoEstado);
}

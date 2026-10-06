package com.findu.dispatcher.infrastructure.adapter.persistence;

import com.findu.dispatcher.domain.model.CanalComunicacion;
import com.findu.dispatcher.domain.model.EstadoCanal;
import com.findu.dispatcher.domain.port.output.ChannelRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de Persistencia Hexagonal para Canales NoSQL (Mongo / DynamoDB).
 * Permite alternar la base de datos subyacente sin modificar la lógica de dominio.
 */
@Slf4j
@Component
public class InMemoryChannelRepositoryAdapter implements ChannelRepositoryPort {

    private final Map<String, CanalComunicacion> dbStore = new ConcurrentHashMap<>();

    @Override
    public Mono<CanalComunicacion> guardar(CanalComunicacion canal) {
        return Mono.fromCallable(() -> {
            dbStore.put(canal.getId(), canal);
            log.debug("Canal guardado en repositorio NoSQL: id={}, estado={}", canal.getId(), canal.getEstado());
            return canal;
        });
    }

    @Override
    public Mono<CanalComunicacion> buscarPorId(String id) {
        return Mono.fromCallable(() -> dbStore.get(id));
    }

    @Override
    public Flux<CanalComunicacion> buscarPorUsuarioId(String usuarioId) {
        return Flux.fromIterable(dbStore.values())
                .filter(c -> c.getInteroperantes().stream().anyMatch(i -> i.usuarioId().equals(usuarioId)));
    }

    @Override
    public Mono<CanalComunicacion> cambiarEstado(String canalId, EstadoCanal nuevoEstado) {
        return buscarPorId(canalId)
                .flatMap(canal -> {
                    canal.setEstado(nuevoEstado);
                    return guardar(canal);
                });
    }
}

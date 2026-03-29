package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.Modulo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para persistencia de Módulo.
 */
public interface ModuloRepositoryPort {

    Mono<Modulo> findById(Long id);

    Mono<Modulo> findByName(String name);

    Flux<Modulo> findByActiveTrue();

    Mono<Modulo> save(Modulo modulo);
}

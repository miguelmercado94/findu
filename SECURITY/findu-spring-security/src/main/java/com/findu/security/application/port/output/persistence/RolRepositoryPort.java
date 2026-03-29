package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.Rol;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para persistencia de Rol.
 */
public interface RolRepositoryPort {

    Mono<Rol> findById(Integer id);

    Mono<Rol> findByName(String name);

    Flux<Rol> findByActiveTrue();

    Mono<Rol> save(Rol rol);
}

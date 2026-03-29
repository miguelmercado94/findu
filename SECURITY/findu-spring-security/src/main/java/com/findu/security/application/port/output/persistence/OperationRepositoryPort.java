package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.Operation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para persistencia de Operation.
 */
public interface OperationRepositoryPort {

    Mono<Operation> findById(Long id);

    Mono<Operation> findByPath(String path);

    Flux<Operation> findByModuleId(Long moduleId);

    Flux<Operation> findByActiveTrue();

    Mono<Operation> save(Operation operation);
}

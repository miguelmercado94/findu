package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.ModuleEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for module table.
 */
public interface ModuleRepository extends R2dbcRepository<ModuleEntity, Long> {

    Mono<ModuleEntity> findByName(String name);

    Flux<ModuleEntity> findByActiveTrue();
}

package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.RoleEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for role table.
 */
public interface RoleRepository extends R2dbcRepository<RoleEntity, Integer> {

    Mono<RoleEntity> findByName(String name);

    Flux<RoleEntity> findByActiveTrue();
}

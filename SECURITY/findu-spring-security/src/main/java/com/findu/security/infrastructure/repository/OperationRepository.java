package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.OperationEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for operation table.
 */
public interface OperationRepository extends R2dbcRepository<OperationEntity, Long> {

    Flux<OperationEntity> findByModuleId(Long moduleId);

    Flux<OperationEntity> findByActiveTrue();

    Mono<OperationEntity> findByPath(String path);
}

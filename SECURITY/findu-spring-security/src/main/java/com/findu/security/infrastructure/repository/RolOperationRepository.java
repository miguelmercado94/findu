package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.RolOperationEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for rol_operation junction table (composite key).
 */
public interface RolOperationRepository {

    Mono<RolOperationEntity> save(RolOperationEntity entity);

    Mono<Void> deleteByRoleIdAndOperationId(Integer roleId, Long operationId);

    Flux<RolOperationEntity> findByRoleId(Integer roleId);

    Flux<RolOperationEntity> findByOperationId(Long operationId);

    Mono<Boolean> existsByRoleIdAndOperationId(Integer roleId, Long operationId);
}

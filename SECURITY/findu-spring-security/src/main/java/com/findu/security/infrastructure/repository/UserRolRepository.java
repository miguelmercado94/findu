package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.UserRolEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for user_rol junction table (composite key).
 */
public interface UserRolRepository {

    Mono<UserRolEntity> save(UserRolEntity entity);

    Mono<Void> deleteByRoleIdAndUserId(Integer roleId, Long userId);

    Flux<UserRolEntity> findByUserId(Long userId);

    Flux<UserRolEntity> findByRoleId(Integer roleId);

    Mono<Boolean> existsByRoleIdAndUserId(Integer roleId, Long userId);
}

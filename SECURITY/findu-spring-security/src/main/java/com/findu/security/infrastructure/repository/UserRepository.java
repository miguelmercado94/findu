package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

/**
 * Repository for user table.
 */
public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<UserEntity> findByUsername(String username);

    Mono<UserEntity> findByEmail(String email);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByPhone(String phone);

    Mono<UserEntity> findByPhone(String phone);

    Mono<UserEntity> findByCodPhoneInternationalAndPhone(String codPhoneInternational, String phone);

    @org.springframework.data.r2dbc.repository.Query("""
            SELECT * FROM "user"
            WHERE estado_activacion = 'EN_REVISION_SOPORTE' AND soporte_notified = FALSE
            """)
    reactor.core.publisher.Flux<UserEntity> findPendingSupportDispatch();
}

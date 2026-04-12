package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.OperationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for operation table.
 */
public interface OperationRepository extends R2dbcRepository<OperationEntity, Long> {

    Flux<OperationEntity> findByModuleId(Long moduleId);

    Flux<OperationEntity> findByActiveTrue();

    Mono<OperationEntity> findByPathAndHttpMethod(String path, String httpMethod);

    Mono<OperationEntity> findByModuleIdAndPathAndHttpMethod(Long moduleId, String path, String httpMethod);

    /**
     * Resuelve la operación por segmento de micro ({@code module.path_base}) + path API + método.
     * Varios módulos pueden compartir el mismo {@code path_base}; el join evita ambigüedad de {@code findByPathBase}.
     */
    @Query("""
            SELECT o.id, o.path, o.name, o.http_method, o.module_id, o.permite_all, o.active,
                   o.created_at, o.updated_at, o.created_by, o.updated_by
            FROM operation o
            INNER JOIN module m ON o.module_id = m.id
            WHERE m.path_base = :pathBase AND m.active = TRUE
              AND o.path = :path AND o.http_method = :httpMethod AND o.active = TRUE
            LIMIT 1
            """)
    Mono<OperationEntity> findByModulePathBaseAndPathAndHttpMethod(String pathBase, String path, String httpMethod);
}

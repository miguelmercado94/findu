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
            SELECT o.ID, o.PATH, o.NAME, o.HTTP_METHOD, o.MODULE_ID, o.PERMITE_ALL, o.ACTIVE,
                   o.CREATED_AT, o.UPDATED_AT, o.CREATED_BY, o.UPDATED_BY
            FROM OPERATION o
            INNER JOIN MODULE m ON o.MODULE_ID = m.ID
            WHERE m.PATH_BASE = :pathBase AND m.ACTIVE = TRUE
              AND o.PATH = :path AND o.HTTP_METHOD = :httpMethod AND o.ACTIVE = TRUE
            FETCH FIRST 1 ROW ONLY
            """)
    Mono<OperationEntity> findByModulePathBaseAndPathAndHttpMethod(String pathBase, String path, String httpMethod);
}

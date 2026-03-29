package com.findu.security.infrastructure.repository.impl;

import com.findu.security.infrastructure.entity.RolOperationEntity;
import com.findu.security.infrastructure.repository.RolOperationRepository;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Implementation for rol_operation table using DatabaseClient (composite key).
 */
@Repository
public class RolOperationRepositoryImpl implements RolOperationRepository {

    private final DatabaseClient databaseClient;

    public RolOperationRepositoryImpl(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<RolOperationEntity> save(RolOperationEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO rol_operation (role_id, operation_id, active, created_at, updated_at, created_by, updated_by)
                VALUES (:roleId, :operationId, :active, :createdAt, :updatedAt, :createdBy, :updatedBy)
                """;
        return databaseClient.sql(sql)
                .bind("roleId", entity.getRoleId())
                .bind("operationId", entity.getOperationId())
                .bind("active", entity.isActive())
                .bind("createdAt", entity.getCreatedAt() != null ? entity.getCreatedAt() : now)
                .bind("updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt() : now)
                .bind("createdBy", entity.getCreatedBy() != null ? entity.getCreatedBy() : "system")
                .bind("updatedBy", entity.getUpdatedBy() != null ? entity.getUpdatedBy() : "system")
                .fetch()
                .rowsUpdated()
                .thenReturn(entity);
    }

    @Override
    public Mono<Void> deleteByRoleIdAndOperationId(Integer roleId, Long operationId) {
        return databaseClient.sql("DELETE FROM rol_operation WHERE role_id = :roleId AND operation_id = :operationId")
                .bind("roleId", roleId)
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<RolOperationEntity> findByRoleId(Integer roleId) {
        return databaseClient.sql("SELECT role_id, operation_id, active, created_at, updated_at, created_by, updated_by FROM rol_operation WHERE role_id = :roleId")
                .bind("roleId", roleId)
                .map((row, meta) -> mapToRolOperation(row))
                .all();
    }

    @Override
    public Flux<RolOperationEntity> findByOperationId(Long operationId) {
        return databaseClient.sql("SELECT role_id, operation_id, active, created_at, updated_at, created_by, updated_by FROM rol_operation WHERE operation_id = :operationId")
                .bind("operationId", operationId)
                .map((row, meta) -> mapToRolOperation(row))
                .all();
    }

    @Override
    public Mono<Boolean> existsByRoleIdAndOperationId(Integer roleId, Long operationId) {
        return databaseClient.sql("SELECT 1 FROM rol_operation WHERE role_id = :roleId AND operation_id = :operationId LIMIT 1")
                .bind("roleId", roleId)
                .bind("operationId", operationId)
                .map((row, meta) -> 1)
                .one()
                .map(r -> true)
                .defaultIfEmpty(false);
    }

    private RolOperationEntity mapToRolOperation(io.r2dbc.spi.Readable row) {
        RolOperationEntity e = new RolOperationEntity();
        e.setRoleId(row.get("role_id", Integer.class));
        e.setOperationId(row.get("operation_id", Long.class));
        e.setActive(Boolean.TRUE.equals(row.get("active", Boolean.class)));
        e.setCreatedAt(row.get("created_at", LocalDateTime.class));
        e.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
        e.setCreatedBy(row.get("created_by", String.class));
        e.setUpdatedBy(row.get("updated_by", String.class));
        return e;
    }
}

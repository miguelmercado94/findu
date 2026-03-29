package com.findu.security.infrastructure.repository.impl;

import com.findu.security.infrastructure.entity.UserRolEntity;
import com.findu.security.infrastructure.repository.UserRolRepository;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Implementation for user_rol table using DatabaseClient (composite key).
 */
@Repository
public class UserRolRepositoryImpl implements UserRolRepository {

    private final DatabaseClient databaseClient;

    public UserRolRepositoryImpl(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<UserRolEntity> save(UserRolEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO user_rol (role_id, user_id, active, created_at, updated_at, created_by, updated_by)
                VALUES (:roleId, :userId, :active, :createdAt, :updatedAt, :createdBy, :updatedBy)
                """;
        return databaseClient.sql(sql)
                .bind("roleId", entity.getRoleId())
                .bind("userId", entity.getUserId())
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
    public Mono<Void> deleteByRoleIdAndUserId(Integer roleId, Long userId) {
        return databaseClient.sql("DELETE FROM user_rol WHERE role_id = :roleId AND user_id = :userId")
                .bind("roleId", roleId)
                .bind("userId", userId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<UserRolEntity> findByUserId(Long userId) {
        return databaseClient.sql("SELECT role_id, user_id, active, created_at, updated_at, created_by, updated_by FROM user_rol WHERE user_id = :userId")
                .bind("userId", userId)
                .map((row, meta) -> mapToUserRol(row))
                .all();
    }

    @Override
    public Flux<UserRolEntity> findByRoleId(Integer roleId) {
        return databaseClient.sql("SELECT role_id, user_id, active, created_at, updated_at, created_by, updated_by FROM user_rol WHERE role_id = :roleId")
                .bind("roleId", roleId)
                .map((row, meta) -> mapToUserRol(row))
                .all();
    }

    @Override
    public Mono<Boolean> existsByRoleIdAndUserId(Integer roleId, Long userId) {
        return databaseClient.sql("SELECT 1 FROM user_rol WHERE role_id = :roleId AND user_id = :userId LIMIT 1")
                .bind("roleId", roleId)
                .bind("userId", userId)
                .map((row, meta) -> 1)
                .one()
                .map(r -> true)
                .defaultIfEmpty(false);
    }

    private UserRolEntity mapToUserRol(io.r2dbc.spi.Readable row) {
        UserRolEntity e = new UserRolEntity();
        e.setRoleId(row.get("role_id", Integer.class));
        e.setUserId(row.get("user_id", Long.class));
        e.setActive(Boolean.TRUE.equals(row.get("active", Boolean.class)));
        e.setCreatedAt(row.get("created_at", LocalDateTime.class));
        e.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
        e.setCreatedBy(row.get("created_by", String.class));
        e.setUpdatedBy(row.get("updated_by", String.class));
        return e;
    }
}

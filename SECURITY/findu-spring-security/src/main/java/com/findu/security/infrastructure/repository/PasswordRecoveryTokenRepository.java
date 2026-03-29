package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.PasswordRecoveryTokenEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository R2DBC para password_recovery_token.
 */
public interface PasswordRecoveryTokenRepository extends R2dbcRepository<PasswordRecoveryTokenEntity, Long> {

    Mono<PasswordRecoveryTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE password_recovery_token SET used = true WHERE token = :token")
    Mono<Integer> markAsUsedByToken(String token);

    @Modifying
    @Query("DELETE FROM password_recovery_token WHERE expires_at < :before")
    Mono<Integer> deleteByExpiresAtBefore(Instant before);
}

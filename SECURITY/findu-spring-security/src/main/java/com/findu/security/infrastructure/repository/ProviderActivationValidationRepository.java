package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.ProviderActivationValidationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio R2DBC de {@code provider_activation_validation}.
 */
public interface ProviderActivationValidationRepository
        extends R2dbcRepository<ProviderActivationValidationEntity, Long> {

    @Query("""
            SELECT * FROM provider_activation_validation
            WHERE user_id = :userId AND tipo = :tipo AND is_current = TRUE
            LIMIT 1
            """)
    Mono<ProviderActivationValidationEntity> findCurrentByUserAndTipo(Long userId, String tipo);

    @Query("""
            SELECT * FROM provider_activation_validation
            WHERE user_id = :userId AND is_current = TRUE
            """)
    Flux<ProviderActivationValidationEntity> findCurrentByUser(Long userId);

    @Modifying
    @Query("""
            UPDATE provider_activation_validation SET is_current = FALSE
            WHERE user_id = :userId AND tipo = :tipo AND is_current = TRUE
            """)
    Mono<Integer> markPreviousNotCurrent(Long userId, String tipo);
}

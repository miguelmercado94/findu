package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.ProviderBiometricCaptureEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio R2DBC de {@code provider_biometric_capture}.
 */
public interface ProviderBiometricCaptureRepository
        extends R2dbcRepository<ProviderBiometricCaptureEntity, Long> {

    @Query("""
            SELECT * FROM provider_biometric_capture
            WHERE user_id = :userId AND resultado = 'OK' AND foto_frontal_url IS NOT NULL
            ORDER BY id DESC
            LIMIT 1
            """)
    Mono<ProviderBiometricCaptureEntity> findLatestOkWithPhoto(Long userId);
}

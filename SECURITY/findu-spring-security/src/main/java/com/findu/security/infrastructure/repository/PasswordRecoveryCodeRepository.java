package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.PasswordRecoveryCodeEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface PasswordRecoveryCodeRepository extends R2dbcRepository<PasswordRecoveryCodeEntity, Long> {
    Flux<PasswordRecoveryCodeEntity> findByUserIdAndUsedFalse(Long userId);
}

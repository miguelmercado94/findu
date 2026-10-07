package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.EmailVerificationTokenEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface EmailVerificationTokenRepository extends R2dbcRepository<EmailVerificationTokenEntity, Long> {
    Mono<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);
}

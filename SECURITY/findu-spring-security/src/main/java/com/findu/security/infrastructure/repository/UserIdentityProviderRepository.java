package com.findu.security.infrastructure.repository;

import com.findu.security.infrastructure.entity.UserIdentityProviderEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserIdentityProviderRepository extends R2dbcRepository<UserIdentityProviderEntity, Long> {
    Mono<UserIdentityProviderEntity> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}

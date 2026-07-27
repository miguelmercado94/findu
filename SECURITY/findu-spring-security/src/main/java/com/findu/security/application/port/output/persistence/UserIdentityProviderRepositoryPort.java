package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.UserIdentityProvider;
import reactor.core.publisher.Mono;

public interface UserIdentityProviderRepositoryPort {
    Mono<UserIdentityProvider> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
    Mono<UserIdentityProvider> save(UserIdentityProvider userIdentityProvider);
}

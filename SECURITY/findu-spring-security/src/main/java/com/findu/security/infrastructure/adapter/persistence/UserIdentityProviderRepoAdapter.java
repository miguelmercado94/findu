package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.UserIdentityProviderRepositoryPort;
import com.findu.security.domain.model.UserIdentityProvider;
import com.findu.security.infrastructure.entity.UserIdentityProviderEntity;
import com.findu.security.infrastructure.repository.UserIdentityProviderRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Component
public class UserIdentityProviderRepoAdapter implements UserIdentityProviderRepositoryPort {

    private final UserIdentityProviderRepository repository;

    public UserIdentityProviderRepoAdapter(UserIdentityProviderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<UserIdentityProvider> findByProviderNameAndProviderUserId(String providerName, String providerUserId) {
        return repository.findByProviderNameAndProviderUserId(providerName, providerUserId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserIdentityProvider> save(UserIdentityProvider domain) {
        UserIdentityProviderEntity entity = toEntity(domain);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(entity)
                .map(this::toDomain);
    }

    private UserIdentityProvider toDomain(UserIdentityProviderEntity entity) {
        return UserIdentityProvider.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .providerName(entity.getProviderName())
                .providerUserId(entity.getProviderUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private UserIdentityProviderEntity toEntity(UserIdentityProvider domain) {
        return new UserIdentityProviderEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getProviderName(),
                domain.getProviderUserId(),
                domain.getCreatedAt()
        );
    }
}

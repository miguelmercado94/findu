package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.ProviderBrebKeyRepositoryPort;
import com.findu.validacion.domain.model.ProviderBrebKey;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.ProviderBrebKeyMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.SpringDataReactiveProviderBrebKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProviderBrebKeyPersistenceAdapter implements ProviderBrebKeyRepositoryPort {

    private final SpringDataReactiveProviderBrebKeyRepository repository;
    private final ProviderBrebKeyMapper mapper;

    @Override
    public Mono<ProviderBrebKey> save(ProviderBrebKey key) {
        return repository.save(mapper.toDocument(key))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<ProviderBrebKey> findActiveByProviderId(Long providerId) {
        return repository.findByProviderIdAndStatus(providerId, ProviderBrebKey.STATUS_ACTIVE)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<ProviderBrebKey> findByProviderId(Long providerId) {
        return repository.findFirstByProviderIdOrderByCreatedAtDesc(providerId)
                .map(mapper::toDomain);
    }
}

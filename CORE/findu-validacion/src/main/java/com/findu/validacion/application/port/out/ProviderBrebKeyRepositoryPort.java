package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.model.ProviderBrebKey;
import reactor.core.publisher.Mono;

public interface ProviderBrebKeyRepositoryPort {
    Mono<ProviderBrebKey> save(ProviderBrebKey key);
    Mono<ProviderBrebKey> findActiveByProviderId(Long providerId);
    Mono<ProviderBrebKey> findByProviderId(Long providerId);
}

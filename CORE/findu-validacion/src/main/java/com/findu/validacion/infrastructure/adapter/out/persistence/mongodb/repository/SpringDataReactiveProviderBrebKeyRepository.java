package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ProviderBrebKeyDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataReactiveProviderBrebKeyRepository
        extends ReactiveMongoRepository<ProviderBrebKeyDocument, String> {

    Mono<ProviderBrebKeyDocument> findByProviderIdAndStatus(Long providerId, String status);

    Mono<ProviderBrebKeyDocument> findFirstByProviderIdOrderByCreatedAtDesc(Long providerId);
}

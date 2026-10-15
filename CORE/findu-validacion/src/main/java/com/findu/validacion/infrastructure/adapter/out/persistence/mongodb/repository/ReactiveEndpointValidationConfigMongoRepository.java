package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.EndpointValidationConfigDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ReactiveEndpointValidationConfigMongoRepository extends ReactiveMongoRepository<EndpointValidationConfigDocument, String> {

    Mono<EndpointValidationConfigDocument> findByProcessCode(String processCode);

    Mono<Boolean> existsByProcessCode(String processCode);
}

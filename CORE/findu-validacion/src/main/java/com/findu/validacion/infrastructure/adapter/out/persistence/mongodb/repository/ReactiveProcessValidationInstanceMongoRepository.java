package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ProcessValidationInstanceDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReactiveProcessValidationInstanceMongoRepository extends ReactiveMongoRepository<ProcessValidationInstanceDocument, String> {

    Mono<ProcessValidationInstanceDocument> findByEntityTypeAndEntityIdAndProcessCodeAndValidationCode(
            String entityType, String entityId, String processCode, String validationCode);

    Flux<ProcessValidationInstanceDocument> findByProcessCodeAndModeAndStatus(
            String processCode, String mode, String status);

    Flux<ProcessValidationInstanceDocument> findByEntityTypeAndEntityIdAndStatus(
            String entityType, String entityId, String status);
}

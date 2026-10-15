package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ValidationDefinitionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface ReactiveValidationDefinitionMongoRepository extends ReactiveMongoRepository<ValidationDefinitionDocument, String> {

    Mono<ValidationDefinitionDocument> findByValidationCode(String validationCode);

    Flux<ValidationDefinitionDocument> findByValidationCodeIn(Collection<String> validationCodes);

    Mono<Boolean> existsByValidationCode(String validationCode);
}

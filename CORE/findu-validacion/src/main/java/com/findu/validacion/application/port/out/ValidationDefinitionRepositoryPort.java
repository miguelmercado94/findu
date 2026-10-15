package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.model.ValidationDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ValidationDefinitionRepositoryPort {
    Mono<ValidationDefinition> findByValidationCode(String validationCode);
    Flux<ValidationDefinition> findByValidationCodeIn(List<String> validationCodes);
    Mono<ValidationDefinition> save(ValidationDefinition definition);
    Flux<ValidationDefinition> findAll();
    Mono<Boolean> existsByValidationCode(String validationCode);
}

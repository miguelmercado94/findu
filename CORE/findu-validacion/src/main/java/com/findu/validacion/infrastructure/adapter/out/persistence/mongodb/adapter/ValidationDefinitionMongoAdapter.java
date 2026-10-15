package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.ValidationDefinitionRepositoryPort;
import com.findu.validacion.domain.model.ValidationDefinition;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.ValidationDefinitionMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveValidationDefinitionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidationDefinitionMongoAdapter implements ValidationDefinitionRepositoryPort {

    private final ReactiveValidationDefinitionMongoRepository repository;
    private final ValidationDefinitionMapper mapper;

    @Override
    public Mono<ValidationDefinition> findByValidationCode(String validationCode) {
        return repository.findByValidationCode(validationCode)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<ValidationDefinition> findByValidationCodeIn(List<String> validationCodes) {
        return repository.findByValidationCodeIn(validationCodes)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<ValidationDefinition> save(ValidationDefinition definition) {
        return repository.save(mapper.toDocument(definition))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<ValidationDefinition> findAll() {
        return repository.findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByValidationCode(String validationCode) {
        return repository.existsByValidationCode(validationCode);
    }
}

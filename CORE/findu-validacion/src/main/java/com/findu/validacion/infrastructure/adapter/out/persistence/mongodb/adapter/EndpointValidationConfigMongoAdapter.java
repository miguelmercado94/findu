package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.EndpointValidationConfigRepositoryPort;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.EndpointValidationConfigMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveEndpointValidationConfigMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EndpointValidationConfigMongoAdapter implements EndpointValidationConfigRepositoryPort {

    private final ReactiveEndpointValidationConfigMongoRepository repository;
    private final EndpointValidationConfigMapper mapper;

    @Override
    public Mono<EndpointValidationConfig> findByProcessCode(String processCode) {
        return repository.findByProcessCode(processCode)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<EndpointValidationConfig> save(EndpointValidationConfig config) {
        return repository.save(mapper.toDocument(config))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByProcessCode(String processCode) {
        return repository.existsByProcessCode(processCode);
    }
}

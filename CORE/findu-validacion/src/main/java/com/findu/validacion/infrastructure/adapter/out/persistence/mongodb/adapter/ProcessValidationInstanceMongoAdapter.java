package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.ProcessValidationInstanceRepositoryPort;
import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.ProcessValidationInstanceMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveProcessValidationInstanceMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProcessValidationInstanceMongoAdapter implements ProcessValidationInstanceRepositoryPort {

    private final ReactiveProcessValidationInstanceMongoRepository repository;
    private final ProcessValidationInstanceMapper mapper;

    @Override
    public Mono<ProcessValidationInstance> save(ProcessValidationInstance instance) {
        return repository.save(mapper.toDocument(instance))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<ProcessValidationInstance> findByEntityAndProcessAndValidation(
            String entityType, String entityId, String processCode, String validationCode) {
        return repository.findByEntityTypeAndEntityIdAndProcessCodeAndValidationCode(
                entityType, entityId, processCode, validationCode)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<ProcessValidationInstance> findPendingManual(String processCode) {
        return repository.findByProcessCodeAndModeAndStatus(
                processCode, ValidationMode.PASSIVE.name(), ValidationStatus.PENDING.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<ProcessValidationInstance> findByEntityAndStatus(String entityType, String entityId, ValidationStatus status) {
        String statusStr = status != null ? status.name() : null;
        return repository.findByEntityTypeAndEntityIdAndStatus(entityType, entityId, statusStr)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<ProcessValidationInstance> findAll() {
        return repository.findAll()
                .map(mapper::toDomain);
    }
}

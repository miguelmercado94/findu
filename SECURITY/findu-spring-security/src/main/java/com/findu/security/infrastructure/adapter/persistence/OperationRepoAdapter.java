package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.OperationRepositoryPort;
import com.findu.security.domain.model.Operation;
import com.findu.security.infrastructure.entity.OperationEntity;
import com.findu.security.infrastructure.repository.OperationRepository;
import com.findu.security.mapper.OperationMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Adapter de persistencia para Operation. Conecta el modelo de dominio con la BD:
 * implementa OperationRepositoryPort, usa OperationMapper (domain ↔ entity) y delega en OperationRepository.
 * Siempre rellena auditoría en creación (createdAt, updatedAt, createdBy, updatedBy) y en actualización (updatedAt, updatedBy).
 */
@Component
public class OperationRepoAdapter implements OperationRepositoryPort {

    private static final String AUDIT_USER = "system";

    private final OperationRepository operationRepository;
    private final OperationMapper operationMapper;

    public OperationRepoAdapter(OperationRepository operationRepository, OperationMapper operationMapper) {
        this.operationRepository = operationRepository;
        this.operationMapper = operationMapper;
    }

    @Override
    public Mono<Operation> findById(Long id) {
        return operationRepository.findById(id)
                .map(operationMapper::toDomain);
    }

    @Override
    public Mono<Operation> findByPath(String path) {
        return operationRepository.findByPath(path)
                .map(operationMapper::toDomain);
    }

    @Override
    public Flux<Operation> findByModuleId(Long moduleId) {
        return operationRepository.findByModuleId(moduleId)
                .map(operationMapper::toDomain);
    }

    @Override
    public Flux<Operation> findByActiveTrue() {
        return operationRepository.findByActiveTrue()
                .map(operationMapper::toDomain);
    }

    @Override
    public Mono<Operation> save(Operation operation) {
        OperationEntity entity = operationMapper.toEntity(operation);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy() : AUDIT_USER);
            entity.setUpdatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy() : AUDIT_USER);
        } else {
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy() : AUDIT_USER);
        }
        return operationRepository.save(entity)
                .map(operationMapper::toDomain);
    }
}

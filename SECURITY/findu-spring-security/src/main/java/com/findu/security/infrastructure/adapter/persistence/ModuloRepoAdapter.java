package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.ModuloRepositoryPort;
import com.findu.security.domain.model.Modulo;
import com.findu.security.infrastructure.entity.ModuleEntity;
import com.findu.security.infrastructure.repository.ModuleRepository;
import com.findu.security.mapper.ModuloMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Adapter de persistencia para Módulo. Conecta el modelo de dominio con la BD:
 * implementa ModuloRepositoryPort, usa ModuloMapper (domain ↔ entity) y delega en ModuleRepository.
 * Siempre rellena auditoría en creación (createdAt, updatedAt, createdBy, updatedBy) y en actualización (updatedAt, updatedBy).
 */
@Component
public class ModuloRepoAdapter implements ModuloRepositoryPort {

    private static final String AUDIT_USER = "system";

    private final ModuleRepository moduleRepository;
    private final ModuloMapper moduloMapper;

    public ModuloRepoAdapter(ModuleRepository moduleRepository, ModuloMapper moduloMapper) {
        this.moduleRepository = moduleRepository;
        this.moduloMapper = moduloMapper;
    }

    @Override
    public Mono<Modulo> findById(Long id) {
        return moduleRepository.findById(id)
                .map(moduloMapper::toDomain);
    }

    @Override
    public Mono<Modulo> findByName(String name) {
        return moduleRepository.findByName(name)
                .map(moduloMapper::toDomain);
    }

    @Override
    public Flux<Modulo> findByActiveTrue() {
        return moduleRepository.findByActiveTrue()
                .map(moduloMapper::toDomain);
    }

    @Override
    public Mono<Modulo> save(Modulo modulo) {
        ModuleEntity entity = moduloMapper.toEntity(modulo);
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
        return moduleRepository.save(entity)
                .map(moduloMapper::toDomain);
    }
}

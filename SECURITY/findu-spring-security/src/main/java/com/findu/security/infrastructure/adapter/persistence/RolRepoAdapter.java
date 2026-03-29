package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.domain.model.Rol;
import com.findu.security.infrastructure.entity.RoleEntity;
import com.findu.security.infrastructure.repository.RoleRepository;
import com.findu.security.mapper.RolMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Adapter de persistencia para Rol. Conecta el modelo de dominio con la BD:
 * implementa RolRepositoryPort, usa RolMapper (domain ↔ entity) y delega en RoleRepository.
 * Siempre rellena auditoría en creación (createdAt, updatedAt, createdBy, updatedBy) y en actualización (updatedAt, updatedBy).
 */
@Component
public class RolRepoAdapter implements RolRepositoryPort {

    private static final String AUDIT_USER = "system";

    private final RoleRepository roleRepository;
    private final RolMapper rolMapper;

    public RolRepoAdapter(RoleRepository roleRepository, RolMapper rolMapper) {
        this.roleRepository = roleRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    public Mono<Rol> findById(Integer id) {
        return roleRepository.findById(id)
                .map(rolMapper::toDomain);
    }

    @Override
    public Mono<Rol> findByName(String name) {
        return roleRepository.findByName(name)
                .map(rolMapper::toDomain);
    }

    @Override
    public Flux<Rol> findByActiveTrue() {
        return roleRepository.findByActiveTrue()
                .map(rolMapper::toDomain);
    }

    @Override
    public Mono<Rol> save(Rol rol) {
        RoleEntity entity = rolMapper.toEntity(rol);
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
        return roleRepository.save(entity)
                .map(rolMapper::toDomain);
    }
}

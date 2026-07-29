package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.infrastructure.repository.PerfilProveedorRepository;
import com.findu.core.mapper.PerfilProveedorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PerfilProveedorRepoAdapter implements PerfilProveedorRepositoryPort {

    private final PerfilProveedorRepository repository;
    private final PerfilProveedorMapper mapper;

    @Override
    public PerfilProveedor save(PerfilProveedor domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<PerfilProveedor> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PerfilProveedor> findByAuthUserId(Long authUserId) {
        return repository.findByAuthUserId(authUserId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByAuthUserId(Long authUserId) {
        return repository.existsByAuthUserId(authUserId);
    }
}

package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.PerfilClienteRepositoryPort;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.infrastructure.repository.PerfilClienteRepository;
import com.findu.core.mapper.PerfilClienteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PerfilClienteRepoAdapter implements PerfilClienteRepositoryPort {

    private final PerfilClienteRepository repository;
    private final PerfilClienteMapper mapper;

    @Override
    public PerfilCliente save(PerfilCliente domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<PerfilCliente> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PerfilCliente> findByAuthUserId(Long authUserId) {
        return repository.findByAuthUserId(authUserId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByAuthUserId(Long authUserId) {
        return repository.existsByAuthUserId(authUserId);
    }

    @Override
    public boolean existsByNumeroIdentificacion(String numeroIdentificacion) {
        return repository.existsByNumeroIdentificacion(numeroIdentificacion);
    }
}

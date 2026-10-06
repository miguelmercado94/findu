package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.EspecialistaCredencialRepositoryPort;
import com.findu.core.domain.model.EspecialistaCredencial;
import com.findu.core.infrastructure.repository.EspecialistaCredencialRepository;
import com.findu.core.mapper.EspecialistaCredencialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EspecialistaCredencialRepoAdapter implements EspecialistaCredencialRepositoryPort {

    private final EspecialistaCredencialRepository repository;
    private final EspecialistaCredencialMapper mapper;

    @Override
    public EspecialistaCredencial save(EspecialistaCredencial domain) {
        return mapper.toDomain(repository.save(mapper.toEntity(domain)));
    }

    @Override
    public Optional<EspecialistaCredencial> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<EspecialistaCredencial> findByEspecialistaId(Long perfilEspecialistaId) {
        return repository.findAllByPerfilEspecialistaId(perfilEspecialistaId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

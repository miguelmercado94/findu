package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.PerfilEspecialistaRepositoryPort;
import com.findu.core.domain.model.PerfilEspecialista;
import com.findu.core.infrastructure.repository.PerfilEspecialistaRepository;
import com.findu.core.mapper.PerfilEspecialistaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PerfilEspecialistaRepoAdapter implements PerfilEspecialistaRepositoryPort {

    private final PerfilEspecialistaRepository repository;
    private final PerfilEspecialistaMapper mapper;

    @Override
    public PerfilEspecialista save(PerfilEspecialista domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<PerfilEspecialista> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PerfilEspecialista> findByProveedorId(Long perfilProveedorId) {
        return repository.findAllByPerfilProveedorIdAndActiveTrue(perfilProveedorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProveedorIdAndServicioId(Long perfilProveedorId, Long servicioId) {
        return repository.existsByPerfilProveedorIdAndServicioId(perfilProveedorId, servicioId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

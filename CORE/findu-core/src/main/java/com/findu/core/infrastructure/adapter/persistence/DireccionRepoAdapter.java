package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.DireccionRepositoryPort;
import com.findu.core.domain.model.Direccion;
import com.findu.core.infrastructure.repository.DireccionRepository;
import com.findu.core.mapper.DireccionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DireccionRepoAdapter implements DireccionRepositoryPort {

    private final DireccionRepository repository;
    private final DireccionMapper mapper;

    @Override
    public Direccion save(Direccion domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Direccion> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Direccion> findByClienteId(Long perfilClienteId) {
        return repository.findAllByPerfilClienteIdAndActiveTrue(perfilClienteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Direccion> findByProveedorId(Long perfilProveedorId) {
        return repository.findAllByPerfilProveedorIdAndActiveTrue(perfilProveedorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Direccion> findBySolicitudId(Long solicitudServicioId) {
        return repository.findBySolicitudServicioId(solicitudServicioId).map(mapper::toDomain);
    }

    @Override
    public long countByClienteId(Long perfilClienteId) {
        return repository.countByPerfilClienteIdAndActiveTrue(perfilClienteId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.OfertaRepositoryPort;
import com.findu.core.domain.model.Oferta;
import com.findu.core.infrastructure.repository.OfertaRepository;
import com.findu.core.mapper.OfertaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OfertaRepoAdapter implements OfertaRepositoryPort {

    private final OfertaRepository repository;
    private final OfertaMapper mapper;

    @Override
    public Oferta save(Oferta domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Oferta> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Oferta> findBySolicitudId(Long solicitudServicioId) {
        return repository.findAllBySolicitudServicioId(solicitudServicioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySolicitudIdAndProveedorId(Long solicitudServicioId, Long perfilProveedorId) {
        return repository.existsBySolicitudServicioIdAndPerfilProveedorId(solicitudServicioId, perfilProveedorId);
    }

    @Override
    public List<Oferta> saveAll(List<Oferta> ofertas) {
        var entities = ofertas.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
}

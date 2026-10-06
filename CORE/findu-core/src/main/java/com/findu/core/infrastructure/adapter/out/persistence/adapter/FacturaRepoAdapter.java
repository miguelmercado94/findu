package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.FacturaRepositoryPort;
import com.findu.core.domain.model.Factura;
import com.findu.core.infrastructure.adapter.out.persistence.repository.FacturaRepository;
import com.findu.core.infrastructure.adapter.out.persistence.mapper.FacturaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FacturaRepoAdapter implements FacturaRepositoryPort {

    private final FacturaRepository repository;
    private final FacturaMapper mapper;

    @Override
    public Factura save(Factura domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Factura> findBySolicitudId(Long solicitudServicioId) {
        return repository.findBySolicitudServicioId(solicitudServicioId).map(mapper::toDomain);
    }
}

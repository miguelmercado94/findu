package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.FacturaDetalleRepositoryPort;
import com.findu.core.domain.model.FacturaDetalle;
import com.findu.core.infrastructure.adapter.out.persistence.repository.FacturaDetalleRepository;
import com.findu.core.infrastructure.adapter.out.persistence.mapper.FacturaDetalleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FacturaDetalleRepoAdapter implements FacturaDetalleRepositoryPort {

    private final FacturaDetalleRepository repository;
    private final FacturaDetalleMapper mapper;

    @Override
    public FacturaDetalle save(FacturaDetalle domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<FacturaDetalle> findByFacturaId(Long facturaId) {
        return repository.findAllByFacturaId(facturaId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.PortafolioItemRepositoryPort;
import com.findu.core.domain.model.PortafolioItem;
import com.findu.core.infrastructure.repository.PortafolioItemRepository;
import com.findu.core.mapper.PortafolioItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PortafolioItemRepoAdapter implements PortafolioItemRepositoryPort {

    private final PortafolioItemRepository repository;
    private final PortafolioItemMapper mapper;

    @Override
    public PortafolioItem save(PortafolioItem domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<PortafolioItem> findByEspecialistaId(Long perfilEspecialistaId) {
        return repository.findAllByPerfilEspecialistaId(perfilEspecialistaId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

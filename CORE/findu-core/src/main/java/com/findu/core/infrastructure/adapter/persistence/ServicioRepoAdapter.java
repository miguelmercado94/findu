package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.ServicioRepositoryPort;
import com.findu.core.domain.model.Servicio;
import com.findu.core.infrastructure.repository.ServicioRepository;
import com.findu.core.mapper.ServicioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServicioRepoAdapter implements ServicioRepositoryPort {

    private final ServicioRepository repository;
    private final ServicioMapper mapper;

    @Override
    public List<Servicio> findAll() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Servicio> findByCategoriaId(Long categoriaId) {
        return repository.findAllByCategoriaIdAndActiveTrue(categoriaId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Servicio> findAll(Pageable pageable) {
        return repository.findAllByActiveTrue(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Servicio> findByCategoriaId(Long categoriaId, Pageable pageable) {
        return repository.findAllByCategoriaIdAndActiveTrue(categoriaId, pageable)
                .map(mapper::toDomain);
    }
}

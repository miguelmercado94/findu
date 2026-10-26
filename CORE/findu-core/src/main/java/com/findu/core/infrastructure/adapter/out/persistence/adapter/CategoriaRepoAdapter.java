package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.domain.model.Categoria;
import com.findu.core.infrastructure.adapter.out.persistence.repository.CategoriaRepository;
import com.findu.core.infrastructure.adapter.out.persistence.mapper.CategoriaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoriaRepoAdapter implements CategoriaRepositoryPort {

    private final CategoriaRepository repository;
    private final CategoriaMapper mapper;

    @Override
    public List<Categoria> findAll() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public java.util.Optional<Categoria> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> findByPadreId(Long categoriaPadreId) {
        return repository.findAllByCategoriaPadreIdAndActiveTrue(categoriaPadreId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Categoria> findRootCategorias(Pageable pageable) {
        return repository.findCategoriesWithActiveServices(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Categoria> findSubcategorias(Long categoriaPadreId, Pageable pageable) {
        return repository.findAllByCategoriaPadreIdAndActiveTrue(categoriaPadreId, pageable)
                .map(mapper::toDomain);
    }
}

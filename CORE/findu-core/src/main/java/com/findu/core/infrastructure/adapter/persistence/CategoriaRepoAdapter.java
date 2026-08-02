package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.CategoriaRepositoryPort;
import com.findu.core.domain.model.Categoria;
import com.findu.core.infrastructure.repository.CategoriaRepository;
import com.findu.core.mapper.CategoriaMapper;
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
    public List<Categoria> findByPadreId(Long categoriaPadreId) {
        return repository.findAllByCategoriaPadreIdAndActiveTrue(categoriaPadreId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Categoria> findRootCategorias(Pageable pageable) {
        return repository.findAllByActiveTrueAndCategoriaPadreIdIsNull(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Categoria> findSubcategorias(Long categoriaPadreId, Pageable pageable) {
        return repository.findAllByCategoriaPadreIdAndActiveTrue(categoriaPadreId, pageable)
                .map(mapper::toDomain);
    }
}

package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoriaRepositoryPort {

    List<Categoria> findAll();

    List<Categoria> findByPadreId(Long categoriaPadreId);

    Page<Categoria> findRootCategorias(Pageable pageable);

    Page<Categoria> findSubcategorias(Long categoriaPadreId, Pageable pageable);
}

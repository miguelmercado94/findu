package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Categoria;

import java.util.List;

public interface CategoriaRepositoryPort {

    List<Categoria> findAll();

    List<Categoria> findByPadreId(Long categoriaPadreId);
}

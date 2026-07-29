package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    List<CategoriaEntity> findAllByActiveTrue();

    List<CategoriaEntity> findAllByCategoriaPadreIdAndActiveTrue(Long categoriaPadreId);
}

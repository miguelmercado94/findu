package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.CategoriaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    List<CategoriaEntity> findAllByActiveTrue();

    Page<CategoriaEntity> findAllByActiveTrueAndCategoriaPadreIdIsNull(Pageable pageable);

    Page<CategoriaEntity> findAllByCategoriaPadreIdAndActiveTrue(Long categoriaPadreId, Pageable pageable);

    List<CategoriaEntity> findAllByCategoriaPadreIdAndActiveTrue(Long categoriaPadreId);
}

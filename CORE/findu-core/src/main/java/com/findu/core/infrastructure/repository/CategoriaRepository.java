package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.CategoriaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {

    List<CategoriaEntity> findAllByActiveTrue();

    @Query("SELECT c FROM CategoriaEntity c WHERE c.active = true AND c.categoriaPadreId IS NULL AND c.id IN (SELECT DISTINCT s.categoriaId FROM ServicioEntity s WHERE s.active = true)")
    Page<CategoriaEntity> findCategoriesWithActiveServices(Pageable pageable);

    Page<CategoriaEntity> findAllByActiveTrueAndCategoriaPadreIdIsNull(Pageable pageable);

    Page<CategoriaEntity> findAllByCategoriaPadreIdAndActiveTrue(Long categoriaPadreId, Pageable pageable);

    List<CategoriaEntity> findAllByCategoriaPadreIdAndActiveTrue(Long categoriaPadreId);
}

package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.ServicioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Long> {

    List<ServicioEntity> findAllByCategoriaIdAndActiveTrue(Long categoriaId);

    Page<ServicioEntity> findAllByCategoriaIdAndActiveTrue(Long categoriaId, Pageable pageable);

    Page<ServicioEntity> findAllByActiveTrue(Pageable pageable);

    List<ServicioEntity> findAllByActiveTrue();
}

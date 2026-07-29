package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.ServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Long> {

    List<ServicioEntity> findAllByCategoriaIdAndActiveTrue(Long categoriaId);

    List<ServicioEntity> findAllByActiveTrue();
}

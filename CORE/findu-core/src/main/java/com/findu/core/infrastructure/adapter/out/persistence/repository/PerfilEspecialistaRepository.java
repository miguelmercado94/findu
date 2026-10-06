package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilEspecialistaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerfilEspecialistaRepository extends JpaRepository<PerfilEspecialistaEntity, Long> {

    List<PerfilEspecialistaEntity> findAllByPerfilProveedorIdAndActiveTrue(Long perfilProveedorId);

    boolean existsByPerfilProveedorIdAndServicioId(Long perfilProveedorId, Long servicioId);
}

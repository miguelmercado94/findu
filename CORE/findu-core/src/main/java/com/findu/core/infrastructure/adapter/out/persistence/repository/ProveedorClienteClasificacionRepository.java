package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.ProveedorClienteClasificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorClienteClasificacionRepository extends JpaRepository<ProveedorClienteClasificacionEntity, Long> {

    Optional<ProveedorClienteClasificacionEntity> findByPerfilProveedorIdAndPerfilClienteId(Long perfilProveedorId,
                                                                                              Long perfilClienteId);
}
package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.DireccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<DireccionEntity, Long> {

    List<DireccionEntity> findAllByPerfilClienteIdAndActiveTrue(Long perfilClienteId);

    List<DireccionEntity> findAllByPerfilProveedorIdAndActiveTrue(Long perfilProveedorId);

    Optional<DireccionEntity> findBySolicitudServicioId(Long solicitudServicioId);

    long countByPerfilClienteIdAndActiveTrue(Long perfilClienteId);

    void deleteAllByPerfilProveedorId(Long perfilProveedorId);

    void deleteAllByPerfilClienteId(Long perfilClienteId);
}

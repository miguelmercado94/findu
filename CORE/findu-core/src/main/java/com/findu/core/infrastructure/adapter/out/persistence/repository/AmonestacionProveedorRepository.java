package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.AmonestacionProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AmonestacionProveedorRepository extends JpaRepository<AmonestacionProveedorEntity, Long> {
    List<AmonestacionProveedorEntity> findByPerfilProveedorIdOrderByCreatedAtDesc(Long perfilProveedorId);
    Optional<AmonestacionProveedorEntity> findByCancelacionId(Long cancelacionId);
    List<AmonestacionProveedorEntity> findByActivaTrue();
}


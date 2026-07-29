package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.PerfilProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilProveedorRepository extends JpaRepository<PerfilProveedorEntity, Long> {

    Optional<PerfilProveedorEntity> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);
}

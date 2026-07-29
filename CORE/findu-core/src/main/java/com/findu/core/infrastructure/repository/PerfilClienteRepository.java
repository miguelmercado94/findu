package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.PerfilClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilClienteRepository extends JpaRepository<PerfilClienteEntity, Long> {

    Optional<PerfilClienteEntity> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);
}

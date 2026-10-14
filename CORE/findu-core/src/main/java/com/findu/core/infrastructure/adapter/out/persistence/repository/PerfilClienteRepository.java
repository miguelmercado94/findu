package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilClienteRepository extends JpaRepository<PerfilClienteEntity, Long> {

    Optional<PerfilClienteEntity> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    Optional<PerfilClienteEntity> findByNumeroIdentificacion(String numeroIdentificacion);
}

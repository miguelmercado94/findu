package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.FacturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<FacturaEntity, Long> {

    Optional<FacturaEntity> findBySolicitudServicioId(Long solicitudServicioId);
}

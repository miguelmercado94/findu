package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.FacturaDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaDetalleRepository extends JpaRepository<FacturaDetalleEntity, Long> {

    List<FacturaDetalleEntity> findAllByFacturaId(Long facturaId);
}

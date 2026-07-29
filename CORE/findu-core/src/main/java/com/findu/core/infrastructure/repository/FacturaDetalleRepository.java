package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.FacturaDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaDetalleRepository extends JpaRepository<FacturaDetalleEntity, Long> {

    List<FacturaDetalleEntity> findAllByFacturaId(Long facturaId);
}

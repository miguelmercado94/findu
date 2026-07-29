package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.FacturaDetalle;

import java.util.List;

public interface FacturaDetalleRepositoryPort {

    FacturaDetalle save(FacturaDetalle facturaDetalle);

    List<FacturaDetalle> findByFacturaId(Long facturaId);

    void deleteById(Long id);
}

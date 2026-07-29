package com.findu.core.application.service;

import com.findu.core.domain.model.FacturaDetalle;

import java.util.List;

public interface FacturaDetalleService {

    FacturaDetalle save(FacturaDetalle facturaDetalle);

    List<FacturaDetalle> findByFacturaId(Long facturaId);

    void deleteById(Long id);
}

package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.FacturaDetalleRepositoryPort;
import com.findu.core.application.service.FacturaDetalleService;
import com.findu.core.domain.model.FacturaDetalle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacturaDetalleServiceImpl implements FacturaDetalleService {

    private final FacturaDetalleRepositoryPort port;

    @Override
    public FacturaDetalle save(FacturaDetalle facturaDetalle) {
        return port.save(facturaDetalle);
    }

    @Override
    public List<FacturaDetalle> findByFacturaId(Long facturaId) {
        return port.findByFacturaId(facturaId);
    }

    @Override
    public void deleteById(Long id) {
        port.deleteById(id);
    }
}

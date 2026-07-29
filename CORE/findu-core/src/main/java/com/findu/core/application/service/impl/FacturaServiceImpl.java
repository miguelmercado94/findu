package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.FacturaRepositoryPort;
import com.findu.core.application.service.FacturaService;
import com.findu.core.domain.model.Factura;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepositoryPort port;

    @Override
    public Factura save(Factura factura) {
        return port.save(factura);
    }

    @Override
    public Optional<Factura> findBySolicitudId(Long solicitudServicioId) {
        return port.findBySolicitudId(solicitudServicioId);
    }
}

package com.findu.core.application.port.out.persistence;

import com.findu.core.domain.model.Factura;

import java.util.Optional;

public interface FacturaRepositoryPort {

    Factura save(Factura factura);

    Optional<Factura> findBySolicitudId(Long solicitudServicioId);
}

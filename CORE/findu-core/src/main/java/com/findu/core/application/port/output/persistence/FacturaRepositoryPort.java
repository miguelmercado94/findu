package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Factura;

import java.util.Optional;

public interface FacturaRepositoryPort {

    Factura save(Factura factura);

    Optional<Factura> findBySolicitudId(Long solicitudServicioId);
}

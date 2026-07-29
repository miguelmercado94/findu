package com.findu.core.application.service;

import com.findu.core.domain.model.Factura;

import java.util.Optional;

public interface FacturaService {

    Factura save(Factura factura);

    Optional<Factura> findBySolicitudId(Long solicitudServicioId);
}

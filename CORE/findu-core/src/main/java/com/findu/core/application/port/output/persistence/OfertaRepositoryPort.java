package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Oferta;

import java.util.List;
import java.util.Optional;

public interface OfertaRepositoryPort {

    Oferta save(Oferta oferta);

    Optional<Oferta> findById(Long id);

    List<Oferta> findBySolicitudId(Long solicitudServicioId);

    boolean existsBySolicitudIdAndProveedorId(Long solicitudServicioId, Long perfilProveedorId);

    List<Oferta> saveAll(List<Oferta> ofertas);
}

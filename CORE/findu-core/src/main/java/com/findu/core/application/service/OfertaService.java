package com.findu.core.application.service;

import com.findu.core.domain.model.Oferta;

import java.util.List;
import java.util.Optional;

public interface OfertaService {

    Oferta save(Oferta oferta);

    Optional<Oferta> findById(Long id);

    List<Oferta> findBySolicitudId(Long solicitudServicioId);

    List<Oferta> findByProveedorId(Long perfilProveedorId);

    Optional<Oferta> findBySolicitudIdAndProveedorId(Long solicitudServicioId, Long perfilProveedorId);

    List<Oferta> findByEstadoOferta(String estadoOferta);

    boolean existsBySolicitudIdAndProveedorId(Long solicitudServicioId, Long perfilProveedorId);

    List<Oferta> saveAll(List<Oferta> ofertas);

    void deleteById(Long id);
}

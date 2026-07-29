package com.findu.core.application.service;

import com.findu.core.domain.model.Direccion;

import java.util.List;
import java.util.Optional;

public interface DireccionService {

    Direccion save(Direccion direccion);

    Optional<Direccion> findById(Long id);

    List<Direccion> findByClienteId(Long perfilClienteId);

    List<Direccion> findByProveedorId(Long perfilProveedorId);

    Optional<Direccion> findBySolicitudId(Long solicitudServicioId);

    long countByClienteId(Long perfilClienteId);

    void deleteById(Long id);
}

package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Direccion;

import java.util.List;
import java.util.Optional;

public interface DireccionRepositoryPort {

    Direccion save(Direccion direccion);

    Optional<Direccion> findById(Long id);

    List<Direccion> findByClienteId(Long perfilClienteId);

    List<Direccion> findByProveedorId(Long perfilProveedorId);

    Optional<Direccion> findBySolicitudId(Long solicitudServicioId);

    long countByClienteId(Long perfilClienteId);

    void deleteById(Long id);
}

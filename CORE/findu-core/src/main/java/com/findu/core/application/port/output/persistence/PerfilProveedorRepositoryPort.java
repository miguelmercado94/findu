package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.PerfilProveedor;

import java.util.Optional;

public interface PerfilProveedorRepositoryPort {

    PerfilProveedor save(PerfilProveedor perfilProveedor);

    Optional<PerfilProveedor> findById(Long id);

    Optional<PerfilProveedor> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);
}

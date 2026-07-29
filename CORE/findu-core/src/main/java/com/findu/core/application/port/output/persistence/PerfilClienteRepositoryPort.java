package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.PerfilCliente;

import java.util.Optional;

public interface PerfilClienteRepositoryPort {

    PerfilCliente save(PerfilCliente perfilCliente);

    Optional<PerfilCliente> findById(Long id);

    Optional<PerfilCliente> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);
}

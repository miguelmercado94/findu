package com.findu.core.application.service;

import com.findu.core.domain.model.PerfilCliente;

import java.util.Optional;

public interface PerfilClienteService {

    PerfilCliente save(PerfilCliente perfilCliente);

    Optional<PerfilCliente> findById(Long id);

    Optional<PerfilCliente> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);
}

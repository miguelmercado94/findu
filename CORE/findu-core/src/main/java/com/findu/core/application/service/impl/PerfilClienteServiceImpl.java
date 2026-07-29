package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.PerfilClienteRepositoryPort;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.domain.model.PerfilCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PerfilClienteServiceImpl implements PerfilClienteService {

    private final PerfilClienteRepositoryPort port;

    @Override
    public PerfilCliente save(PerfilCliente perfilCliente) {
        return port.save(perfilCliente);
    }

    @Override
    public Optional<PerfilCliente> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public Optional<PerfilCliente> findByAuthUserId(Long authUserId) {
        return port.findByAuthUserId(authUserId);
    }

    @Override
    public boolean existsByAuthUserId(Long authUserId) {
        return port.existsByAuthUserId(authUserId);
    }

    @Override
    public boolean existsByNumeroIdentificacion(String numeroIdentificacion) {
        return port.existsByNumeroIdentificacion(numeroIdentificacion);
    }
}

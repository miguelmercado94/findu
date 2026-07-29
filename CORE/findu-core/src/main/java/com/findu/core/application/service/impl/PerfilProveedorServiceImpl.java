package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.domain.model.PerfilProveedor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PerfilProveedorServiceImpl implements PerfilProveedorService {

    private final PerfilProveedorRepositoryPort port;

    @Override
    public PerfilProveedor save(PerfilProveedor perfilProveedor) {
        return port.save(perfilProveedor);
    }

    @Override
    public Optional<PerfilProveedor> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public Optional<PerfilProveedor> findByAuthUserId(Long authUserId) {
        return port.findByAuthUserId(authUserId);
    }

    @Override
    public boolean existsByAuthUserId(Long authUserId) {
        return port.existsByAuthUserId(authUserId);
    }
}

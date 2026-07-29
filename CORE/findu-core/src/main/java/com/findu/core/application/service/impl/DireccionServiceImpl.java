package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.DireccionRepositoryPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.domain.model.Direccion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DireccionServiceImpl implements DireccionService {

    private final DireccionRepositoryPort port;

    @Override
    public Direccion save(Direccion direccion) {
        return port.save(direccion);
    }

    @Override
    public Optional<Direccion> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public List<Direccion> findByClienteId(Long perfilClienteId) {
        return port.findByClienteId(perfilClienteId);
    }

    @Override
    public List<Direccion> findByProveedorId(Long perfilProveedorId) {
        return port.findByProveedorId(perfilProveedorId);
    }

    @Override
    public Optional<Direccion> findBySolicitudId(Long solicitudServicioId) {
        return port.findBySolicitudId(solicitudServicioId);
    }

    @Override
    public long countByClienteId(Long perfilClienteId) {
        return port.countByClienteId(perfilClienteId);
    }

    @Override
    public void deleteById(Long id) {
        port.deleteById(id);
    }
}

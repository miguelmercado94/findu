package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.SolicitudServicioRepositoryPort;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.domain.model.SolicitudServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SolicitudServicioServiceImpl implements SolicitudServicioService {

    private final SolicitudServicioRepositoryPort port;

    @Override
    public SolicitudServicio save(SolicitudServicio solicitud) {
        return port.save(solicitud);
    }

    @Override
    public Optional<SolicitudServicio> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public List<SolicitudServicio> findByClienteId(Long perfilClienteId) {
        return port.findByClienteId(perfilClienteId);
    }

    @Override
    public boolean existsByClienteIdAndEstadosActivos(Long perfilClienteId, List<String> estados) {
        return port.existsByClienteIdAndEstados(perfilClienteId, estados);
    }

    @Override
    public List<SolicitudServicio> findByEstado(String estado) {
        return port.findByEstado(estado);
    }
}

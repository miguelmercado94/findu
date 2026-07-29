package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.OfertaRepositoryPort;
import com.findu.core.application.service.OfertaService;
import com.findu.core.domain.model.Oferta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfertaServiceImpl implements OfertaService {

    private final OfertaRepositoryPort port;

    @Override
    public Oferta save(Oferta oferta) {
        return port.save(oferta);
    }

    @Override
    public Optional<Oferta> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public List<Oferta> findBySolicitudId(Long solicitudServicioId) {
        return port.findBySolicitudId(solicitudServicioId);
    }

    @Override
    public boolean existsBySolicitudIdAndProveedorId(Long solicitudServicioId, Long perfilProveedorId) {
        return port.existsBySolicitudIdAndProveedorId(solicitudServicioId, perfilProveedorId);
    }

    @Override
    public List<Oferta> saveAll(List<Oferta> ofertas) {
        return port.saveAll(ofertas);
    }
}

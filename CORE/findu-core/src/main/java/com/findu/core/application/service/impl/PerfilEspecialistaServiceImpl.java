package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.PerfilEspecialistaRepositoryPort;
import com.findu.core.application.service.PerfilEspecialistaService;
import com.findu.core.domain.model.PerfilEspecialista;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PerfilEspecialistaServiceImpl implements PerfilEspecialistaService {

    private final PerfilEspecialistaRepositoryPort port;

    @Override
    public PerfilEspecialista save(PerfilEspecialista perfilEspecialista) {
        return port.save(perfilEspecialista);
    }

    @Override
    public Optional<PerfilEspecialista> findById(Long id) {
        return port.findById(id);
    }

    @Override
    public List<PerfilEspecialista> findByProveedorId(Long perfilProveedorId) {
        return port.findByProveedorId(perfilProveedorId);
    }

    @Override
    public boolean existsByProveedorIdAndServicioId(Long perfilProveedorId, Long servicioId) {
        return port.existsByProveedorIdAndServicioId(perfilProveedorId, servicioId);
    }

    @Override
    public void deleteById(Long id) {
        port.deleteById(id);
    }
}

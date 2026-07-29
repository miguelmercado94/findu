package com.findu.core.application.service;

import com.findu.core.domain.model.PerfilEspecialista;

import java.util.List;
import java.util.Optional;

public interface PerfilEspecialistaService {

    PerfilEspecialista save(PerfilEspecialista perfilEspecialista);

    Optional<PerfilEspecialista> findById(Long id);

    List<PerfilEspecialista> findByProveedorId(Long perfilProveedorId);

    boolean existsByProveedorIdAndServicioId(Long perfilProveedorId, Long servicioId);

    void deleteById(Long id);
}

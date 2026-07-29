package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.SolicitudServicio;

import java.util.List;
import java.util.Optional;

public interface SolicitudServicioRepositoryPort {

    SolicitudServicio save(SolicitudServicio solicitud);

    Optional<SolicitudServicio> findById(Long id);

    List<SolicitudServicio> findByClienteId(Long perfilClienteId);

    boolean existsByClienteIdAndEstados(Long perfilClienteId, List<String> estados);

    List<SolicitudServicio> findByEstado(String estado);
}

package com.findu.core.application.service;

import com.findu.core.domain.model.SolicitudServicio;

import java.util.List;
import java.util.Optional;

public interface SolicitudServicioService {

    SolicitudServicio save(SolicitudServicio solicitud);

    Optional<SolicitudServicio> findById(Long id);

    List<SolicitudServicio> findByClienteId(Long perfilClienteId);

    boolean existsByClienteIdAndEstadosActivos(Long perfilClienteId, List<String> estados);

    List<SolicitudServicio> findByEstado(String estado);
}

package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.SolicitudServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SolicitudServicioRepositoryPort {

    SolicitudServicio save(SolicitudServicio solicitud);

    Optional<SolicitudServicio> findById(Long id);

    List<SolicitudServicio> findByClienteId(Long perfilClienteId);

    Page<SolicitudServicio> findByClienteId(Long perfilClienteId, Pageable pageable);

    Page<SolicitudServicio> findByClienteIdAndEstado(Long perfilClienteId, String estado, Pageable pageable);

    boolean existsByClienteIdAndEstados(Long perfilClienteId, List<String> estados);

    List<SolicitudServicio> findByEstado(String estado);
}

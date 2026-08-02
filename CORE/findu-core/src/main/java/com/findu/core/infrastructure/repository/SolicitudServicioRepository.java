package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.SolicitudServicioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicioEntity, Long> {

    List<SolicitudServicioEntity> findAllByPerfilClienteIdOrderByCreatedAtDesc(Long perfilClienteId);

    Page<SolicitudServicioEntity> findAllByPerfilClienteIdOrderByCreatedAtDesc(Long perfilClienteId, Pageable pageable);

    Page<SolicitudServicioEntity> findAllByPerfilClienteIdAndEstadoSolicitudOrderByCreatedAtDesc(Long perfilClienteId, String estadoSolicitud, Pageable pageable);

    List<SolicitudServicioEntity> findAllByEstadoSolicitud(String estadoSolicitud);

    boolean existsByPerfilClienteIdAndEstadoSolicitudIn(Long perfilClienteId, List<String> estados);
}

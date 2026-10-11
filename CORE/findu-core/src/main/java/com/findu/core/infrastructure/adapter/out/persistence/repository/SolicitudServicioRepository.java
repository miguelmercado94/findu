package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.SolicitudServicioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicioEntity, Long> {

    @Query("SELECT s FROM SolicitudServicioEntity s " +
            "WHERE s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o " +
            "               WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA') " +
            "AND (:estado IS NULL OR s.estadoSolicitud = :estado) " +
            "ORDER BY s.fechaProgramada DESC")
    List<SolicitudServicioEntity> findByProveedorAceptadaAndEstado(@Param("proveedorId") Long proveedorId,
                                                                   @Param("estado") String estado);

    List<SolicitudServicioEntity> findAllByPerfilClienteIdOrderByCreatedAtDesc(Long perfilClienteId);

    Page<SolicitudServicioEntity> findAllByPerfilClienteIdOrderByCreatedAtDesc(Long perfilClienteId, Pageable pageable);

    Page<SolicitudServicioEntity> findAllByPerfilClienteIdAndEstadoSolicitudOrderByCreatedAtDesc(Long perfilClienteId, String estadoSolicitud, Pageable pageable);

    List<SolicitudServicioEntity> findAllByEstadoSolicitud(String estadoSolicitud);

    boolean existsByPerfilClienteIdAndEstadoSolicitudIn(Long perfilClienteId, List<String> estados);
}

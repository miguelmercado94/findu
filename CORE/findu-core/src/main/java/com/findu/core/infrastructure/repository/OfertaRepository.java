package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.OfertaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<OfertaEntity, Long> {

    List<OfertaEntity> findAllBySolicitudServicioId(Long solicitudServicioId);

    boolean existsBySolicitudServicioIdAndPerfilProveedorId(Long solicitudServicioId, Long perfilProveedorId);

    List<OfertaEntity> findAllBySolicitudServicioIdAndEstadoOferta(Long solicitudServicioId, String estadoOferta);
}

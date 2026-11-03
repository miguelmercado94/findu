package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.SolicitudEstadoAuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudEstadoAuditoriaRepository extends JpaRepository<SolicitudEstadoAuditoriaEntity, Long> {
    List<SolicitudEstadoAuditoriaEntity> findBySolicitudServicioIdOrderByFechaHoraAsc(Long solicitudServicioId);
}


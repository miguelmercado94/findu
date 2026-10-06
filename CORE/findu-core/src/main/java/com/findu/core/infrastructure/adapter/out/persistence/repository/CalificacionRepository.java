package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.CalificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<CalificacionEntity, Long> {

    List<CalificacionEntity> findAllByEvaluadoId(Long evaluadoId);

    boolean existsBySolicitudServicioIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId);
}

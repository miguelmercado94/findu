package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.CalificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<CalificacionEntity, Long> {

    List<CalificacionEntity> findAllByEvaluadoId(Long evaluadoId);

    boolean existsBySolicitudServicioIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId);
}

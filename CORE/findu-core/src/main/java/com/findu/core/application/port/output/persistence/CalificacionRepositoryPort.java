package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Calificacion;

import java.util.List;

public interface CalificacionRepositoryPort {

    Calificacion save(Calificacion calificacion);

    List<Calificacion> findByEvaluadoId(Long evaluadoId);

    boolean existsBySolicitudIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId);
}

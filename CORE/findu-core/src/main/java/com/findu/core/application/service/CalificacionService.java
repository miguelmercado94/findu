package com.findu.core.application.service;

import com.findu.core.domain.model.Calificacion;

import java.util.List;

public interface CalificacionService {

    Calificacion save(Calificacion calificacion);

    List<Calificacion> findByEvaluadoId(Long evaluadoId);

    boolean existsBySolicitudIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId);
}

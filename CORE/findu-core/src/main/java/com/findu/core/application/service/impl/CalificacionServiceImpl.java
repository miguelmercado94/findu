package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.CalificacionRepositoryPort;
import com.findu.core.application.service.CalificacionService;
import com.findu.core.domain.model.Calificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalificacionServiceImpl implements CalificacionService {

    private final CalificacionRepositoryPort port;

    @Override
    public Calificacion save(Calificacion calificacion) {
        return port.save(calificacion);
    }

    @Override
    public List<Calificacion> findByEvaluadoId(Long evaluadoId) {
        return port.findByEvaluadoId(evaluadoId);
    }

    @Override
    public boolean existsBySolicitudIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId) {
        return port.existsBySolicitudIdAndEvaluadorId(solicitudServicioId, evaluadorId);
    }
}

package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.CalificacionRepositoryPort;
import com.findu.core.domain.model.Calificacion;
import com.findu.core.infrastructure.repository.CalificacionRepository;
import com.findu.core.mapper.CalificacionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CalificacionRepoAdapter implements CalificacionRepositoryPort {

    private final CalificacionRepository repository;
    private final CalificacionMapper mapper;

    @Override
    public Calificacion save(Calificacion domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<Calificacion> findByEvaluadoId(Long evaluadoId) {
        return repository.findAllByEvaluadoId(evaluadoId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySolicitudIdAndEvaluadorId(Long solicitudServicioId, Long evaluadorId) {
        return repository.existsBySolicitudServicioIdAndEvaluadorId(solicitudServicioId, evaluadorId);
    }
}

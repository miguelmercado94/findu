package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.SolicitudServicioRepositoryPort;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.infrastructure.repository.SolicitudServicioRepository;
import com.findu.core.mapper.SolicitudServicioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SolicitudServicioRepoAdapter implements SolicitudServicioRepositoryPort {

    private final SolicitudServicioRepository repository;
    private final SolicitudServicioMapper mapper;

    @Override
    public SolicitudServicio save(SolicitudServicio domain) {
        var entity = mapper.toEntity(domain);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<SolicitudServicio> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SolicitudServicio> findByClienteId(Long perfilClienteId) {
        return repository.findAllByPerfilClienteIdOrderByCreatedAtDesc(perfilClienteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<SolicitudServicio> findByClienteId(Long perfilClienteId, Pageable pageable) {
        return repository.findAllByPerfilClienteIdOrderByCreatedAtDesc(perfilClienteId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SolicitudServicio> findByClienteIdAndEstado(Long perfilClienteId, String estado, Pageable pageable) {
        return repository.findAllByPerfilClienteIdAndEstadoSolicitudOrderByCreatedAtDesc(perfilClienteId, estado, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByClienteIdAndEstados(Long perfilClienteId, List<String> estados) {
        return repository.existsByPerfilClienteIdAndEstadoSolicitudIn(perfilClienteId, estados);
    }

    @Override
    public List<SolicitudServicio> findByEstado(String estado) {
        return repository.findAllByEstadoSolicitud(estado).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

package com.findu.core.infrastructure.adapter.persistence;

import com.findu.core.application.port.output.persistence.ProveedorCoberturaRepositoryPort;
import com.findu.core.domain.model.ProveedorCobertura;
import com.findu.core.infrastructure.entity.ProveedorCoberturaEntity;
import com.findu.core.infrastructure.repository.ProveedorCoberturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProveedorCoberturaRepoAdapter implements ProveedorCoberturaRepositoryPort {

    private final ProveedorCoberturaRepository repository;

    @Override
    @Transactional
    public void saveAll(Long proveedorId, List<Long> municipioIds) {
        List<ProveedorCoberturaEntity> entities = municipioIds.stream()
                .map(municipioId -> ProveedorCoberturaEntity.builder()
                        .perfilProveedorId(proveedorId)
                        .municipioId(municipioId)
                        .build())
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public List<ProveedorCobertura> findByProveedorId(Long perfilProveedorId) {
        return repository.findAllByPerfilProveedorId(perfilProveedorId).stream()
                .map(e -> ProveedorCobertura.builder()
                        .id(e.getId())
                        .perfilProveedorId(e.getPerfilProveedorId())
                        .municipioId(e.getMunicipioId())
                        .build())
                .toList();
    }

    @Override
    public List<ProveedorCobertura> findProveedoresByMunicipioId(Long municipioId) {
        return repository.findAllByMunicipioId(municipioId).stream()
                .map(e -> ProveedorCobertura.builder()
                        .id(e.getId())
                        .perfilProveedorId(e.getPerfilProveedorId())
                        .municipioId(e.getMunicipioId())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByProveedorId(Long perfilProveedorId) {
        repository.deleteAllByPerfilProveedorId(perfilProveedorId);
    }
}

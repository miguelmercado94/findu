package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.ClasificacionClienteRepositoryPort;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ProveedorClienteClasificacionEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.ProveedorClienteClasificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClasificacionClienteRepoAdapter implements ClasificacionClienteRepositoryPort {

    private final ProveedorClienteClasificacionRepository repository;

    @Override
    public void guardarFavoritoInterno(Long perfilProveedorId, Long perfilClienteId, boolean favoritoInterno) {
        ProveedorClienteClasificacionEntity entity = repository
                .findByPerfilProveedorIdAndPerfilClienteId(perfilProveedorId, perfilClienteId)
                .orElseGet(() -> ProveedorClienteClasificacionEntity.builder()
                        .perfilProveedorId(perfilProveedorId)
                        .perfilClienteId(perfilClienteId)
                        .build());
        entity.setFavoritoInterno(favoritoInterno);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }
}
package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import com.findu.core.infrastructure.adapter.out.persistence.entity.ClienteProveedorFavoritoEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.ClienteProveedorFavoritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClienteProveedorFavoritoRepoAdapter implements ClienteProveedorFavoritoRepositoryPort {

    private final ClienteProveedorFavoritoRepository repository;

    @Override
    public ClienteProveedorFavorito save(ClienteProveedorFavorito domain) {
        LocalDateTime now = LocalDateTime.now();
        ClienteProveedorFavoritoEntity entity = domain.getId() == null
                ? new ClienteProveedorFavoritoEntity()
                : repository.findById(domain.getId()).orElseGet(ClienteProveedorFavoritoEntity::new);
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
            entity.setClienteRef(domain.getClienteRef() != null ? domain.getClienteRef() : UUID.randomUUID());
        }
        entity.setPerfilClienteId(domain.getPerfilClienteId());
        entity.setPerfilProveedorId(domain.getPerfilProveedorId());
        entity.setActivo(domain.isActivo());
        entity.setFavoritoDesde(domain.getFavoritoDesde());
        entity.setRemovidoAt(domain.getRemovidoAt());
        entity.setUpdatedAt(now);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<ClienteProveedorFavorito> findByClienteAndProveedor(Long perfilClienteId, Long perfilProveedorId) {
        return repository.findByPerfilClienteIdAndPerfilProveedorId(perfilClienteId, perfilProveedorId)
                .map(this::toDomain);
    }

    @Override
    public Optional<ClienteProveedorFavorito> findByProveedorAndClienteRef(Long perfilProveedorId, UUID clienteRef) {
        return repository.findByPerfilProveedorIdAndClienteRef(perfilProveedorId, clienteRef).map(this::toDomain);
    }

    @Override
    public List<ClienteProveedorFavorito> findAllByCliente(Long perfilClienteId) {
        return repository.findAllByPerfilClienteIdOrderByIdAsc(perfilClienteId).stream().map(this::toDomain).toList();
    }

    private ClienteProveedorFavorito toDomain(ClienteProveedorFavoritoEntity e) {
        return ClienteProveedorFavorito.builder()
                .id(e.getId())
                .perfilClienteId(e.getPerfilClienteId())
                .perfilProveedorId(e.getPerfilProveedorId())
                .clienteRef(e.getClienteRef())
                .activo(e.isActivo())
                .favoritoDesde(e.getFavoritoDesde())
                .removidoAt(e.getRemovidoAt())
                .build();
    }
}
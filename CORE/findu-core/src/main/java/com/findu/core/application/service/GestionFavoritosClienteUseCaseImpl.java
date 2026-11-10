package com.findu.core.application.service;

import com.findu.core.application.port.in.GestionFavoritosClienteUseCase;
import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.PerfilProveedorRepositoryPort;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionFavoritosClienteUseCaseImpl implements GestionFavoritosClienteUseCase {

    private final ClienteProveedorFavoritoRepositoryPort favoritoPort;
    private final PerfilProveedorRepositoryPort proveedorPort;

    @Override
    public void marcarFavorito(Long perfilClienteId, Long perfilProveedorId) {
        if (proveedorPort.findById(perfilProveedorId).isEmpty()) {
            throw new ResourceNotFoundException("Proveedor", perfilProveedorId);
        }
        ClienteProveedorFavorito existente = favoritoPort.findByClienteAndProveedor(perfilClienteId, perfilProveedorId)
                .orElse(null);
        if (existente == null) {
            favoritoPort.save(ClienteProveedorFavorito.builder()
                    .perfilClienteId(perfilClienteId)
                    .perfilProveedorId(perfilProveedorId)
                    .clienteRef(UUID.randomUUID())
                    .activo(true)
                    .favoritoDesde(LocalDateTime.now())
                    .build());
        } else if (!existente.isActivo()) {
            existente.setActivo(true);
            existente.setFavoritoDesde(LocalDateTime.now());
            existente.setRemovidoAt(null);
            favoritoPort.save(existente);
        }
    }

    @Override
    public void quitarFavorito(Long perfilClienteId, Long perfilProveedorId) {
        favoritoPort.findByClienteAndProveedor(perfilClienteId, perfilProveedorId)
                .filter(ClienteProveedorFavorito::isActivo)
                .ifPresent(f -> {
                    f.setActivo(false);
                    f.setRemovidoAt(LocalDateTime.now());
                    favoritoPort.save(f);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoritoProveedor> listar(Long perfilClienteId) {
        return favoritoPort.findAllByCliente(perfilClienteId).stream()
                .map(f -> new FavoritoProveedor(f.getPerfilProveedorId(), f.isActivo()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FavoritoProveedor obtener(Long perfilClienteId, Long perfilProveedorId) {
        return favoritoPort.findByClienteAndProveedor(perfilClienteId, perfilProveedorId)
                .map(f -> new FavoritoProveedor(f.getPerfilProveedorId(), f.isActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Favorito", perfilProveedorId));
    }
}
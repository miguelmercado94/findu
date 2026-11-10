package com.findu.core.application.port.out.persistence;

import com.findu.core.domain.model.ClienteProveedorFavorito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteProveedorFavoritoRepositoryPort {

    ClienteProveedorFavorito save(ClienteProveedorFavorito favorito);

    Optional<ClienteProveedorFavorito> findByClienteAndProveedor(Long perfilClienteId, Long perfilProveedorId);

    /** Resuelve el id opaco SOLO dentro del proveedor dado: un ref de otro proveedor no resuelve. */
    Optional<ClienteProveedorFavorito> findByProveedorAndClienteRef(Long perfilProveedorId, UUID clienteRef);

    List<ClienteProveedorFavorito> findAllByCliente(Long perfilClienteId);
}
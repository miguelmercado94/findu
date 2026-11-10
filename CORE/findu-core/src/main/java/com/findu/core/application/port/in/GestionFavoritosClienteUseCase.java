package com.findu.core.application.port.in;

import java.util.List;

public interface GestionFavoritosClienteUseCase {

    record FavoritoProveedor(Long proveedorId, boolean activo) {
    }

    /** Crea el vinculo o lo reactiva (idempotente). 404 si el proveedor no existe. */
    void marcarFavorito(Long perfilClienteId, Long perfilProveedorId);

    /** Baja logica (activo=false); nunca borra la fila; idempotente. */
    void quitarFavorito(Long perfilClienteId, Long perfilProveedorId);

    List<FavoritoProveedor> listar(Long perfilClienteId);

    /** 404 si el cliente nunca marco a ese proveedor. */
    FavoritoProveedor obtener(Long perfilClienteId, Long perfilProveedorId);
}
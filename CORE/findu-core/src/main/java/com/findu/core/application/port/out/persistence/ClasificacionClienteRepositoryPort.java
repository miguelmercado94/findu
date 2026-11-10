package com.findu.core.application.port.out.persistence;

public interface ClasificacionClienteRepositoryPort {

    /** Crea o actualiza (upsert, idempotente) la marca privada del proveedor sobre un cliente. */
    void guardarFavoritoInterno(Long perfilProveedorId, Long perfilClienteId, boolean favoritoInterno);
}
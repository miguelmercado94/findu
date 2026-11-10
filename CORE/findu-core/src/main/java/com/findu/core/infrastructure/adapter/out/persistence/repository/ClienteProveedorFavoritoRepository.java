package com.findu.core.infrastructure.adapter.out.persistence.repository;

import com.findu.core.infrastructure.adapter.out.persistence.entity.ClienteProveedorFavoritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteProveedorFavoritoRepository extends JpaRepository<ClienteProveedorFavoritoEntity, Long> {

    Optional<ClienteProveedorFavoritoEntity> findByPerfilClienteIdAndPerfilProveedorId(Long perfilClienteId,
                                                                                         Long perfilProveedorId);

    Optional<ClienteProveedorFavoritoEntity> findByPerfilProveedorIdAndClienteRef(Long perfilProveedorId, UUID clienteRef);

    List<ClienteProveedorFavoritoEntity> findAllByPerfilClienteIdOrderByIdAsc(Long perfilClienteId);
}
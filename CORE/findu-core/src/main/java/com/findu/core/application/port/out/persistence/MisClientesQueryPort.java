package com.findu.core.application.port.out.persistence;

import com.findu.core.domain.model.ClienteVinculoResumen;
import com.findu.core.domain.model.ServicioHistorialItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Lecturas de "Mis clientes". Todas parten del vinculo del proveedor: sin vinculo no hay resultado. */
public interface MisClientesQueryPort {

    /** Orden: en servicio, favorito interno, resto; luego nombre y id. Incluye vinculos inactivos. */
    Page<ClienteVinculoResumen> listar(Long perfilProveedorId, String textoBusqueda, Pageable pageable);

    Optional<ClienteVinculoResumen> detalle(Long perfilProveedorId, UUID clienteRef);

    /** Solo solicitudes con oferta ACEPTADA de ese proveedor, mas recientes primero. */
    Page<ServicioHistorialItem> historial(Long perfilProveedorId, Long perfilClienteId, Pageable pageable);

    /** Contenido crudo de perfil_cliente.url_imagen_perfil. */
    Optional<String> fotoCliente(Long perfilClienteId);
}
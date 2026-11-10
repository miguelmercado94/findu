package com.findu.core.application.port.in;

import com.findu.core.application.dto.response.ClienteDetalleResponse;
import com.findu.core.application.dto.response.ClienteResumenResponse;
import com.findu.core.application.dto.response.PaginaResponse;
import com.findu.core.application.dto.response.ServicioHistorialItemResponse;

import java.util.UUID;

/**
 * "Mis clientes" del proveedor. El proveedor siempre llega resuelto desde el JWT; sin vinculo todo es 404
 * (ResourceNotFoundException), identico a "no existe".
 */
public interface GestionMisClientesUseCase {

    int TAMANO_MAXIMO = 50;

    record FotoCliente(byte[] bytes, String contentType) {
    }

    PaginaResponse<ClienteResumenResponse> listar(Long perfilProveedorId, String q, int page, int size);

    ClienteDetalleResponse detalle(Long perfilProveedorId, UUID clienteRef);

    PaginaResponse<ServicioHistorialItemResponse> historial(Long perfilProveedorId, UUID clienteRef, int page, int size);

    FotoCliente foto(Long perfilProveedorId, UUID clienteRef);

    /** Idempotente. Solo toca proveedor_cliente_clasificacion; funciona tambien con vinculo inactivo. */
    void definirFavoritoInterno(Long perfilProveedorId, UUID clienteRef, boolean favorito);
}
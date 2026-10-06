package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.ActualizarPerfilProveedorRequest;
import com.findu.core.application.dto.request.CrearPerfilProveedorRequest;
import com.findu.core.application.dto.response.PerfilProveedorDetalleResponse;
import com.findu.core.application.dto.response.PerfilProveedorPublicoResponse;
import com.findu.core.application.dto.response.PerfilProveedorResponse;

import java.util.List;

public interface GestionPerfilProveedorUseCase {

    PerfilProveedorResponse crearPerfil(CrearPerfilProveedorRequest request);

    PerfilProveedorDetalleResponse consultarPerfil(Long id);

    PerfilProveedorDetalleResponse consultarPerfilPorAuthUserId(Long authUserId);

    /**
     * Vista pública del proveedor para el cliente.
     * Filtra por servicioId para mostrar solo la especialidad relevante.
     *
     * @param proveedorId ID del perfil proveedor
     * @param servicioId  ID del servicio para filtrar la especialidad mostrada
     */
    PerfilProveedorPublicoResponse consultarPerfilPublico(Long proveedorId, Long servicioId);

    PerfilProveedorResponse actualizarPerfil(Long id, ActualizarPerfilProveedorRequest request);

    void cambiarEstado(Long id, String nuevoEstado);

    void actualizarCobertura(Long id, List<String> codigosDane);

    PerfilProveedorResponse actualizarDisponibilidad(Long id, boolean disponible);
}

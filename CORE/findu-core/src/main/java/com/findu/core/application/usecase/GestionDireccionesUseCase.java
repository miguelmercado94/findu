package com.findu.core.application.usecase;

import com.findu.core.dto.request.ActualizarDireccionRequest;
import com.findu.core.dto.request.CrearDireccionClienteRequest;
import com.findu.core.dto.request.CrearDireccionRequest;
import com.findu.core.dto.response.DireccionResponse;

import java.util.List;

public interface GestionDireccionesUseCase {

    DireccionResponse crearDireccion(CrearDireccionRequest request);

    DireccionResponse crearDireccionCliente(Long perfilClienteId, CrearDireccionClienteRequest request);

    DireccionResponse actualizarDireccion(Long id, ActualizarDireccionRequest request);

    void eliminarDireccion(Long id);

    DireccionResponse consultarDireccion(Long id);

    List<DireccionResponse> listarDireccionesCliente(Long clienteId);

    DireccionResponse obtenerDireccionSolicitud(Long solicitudId);

    DireccionResponse marcarComoPrincipal(Long direccionId);
}

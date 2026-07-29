package com.findu.core.application.usecase;

import com.findu.core.dto.request.CrearSolicitudRequest;
import com.findu.core.dto.request.ModificarSolicitudRequest;
import com.findu.core.dto.response.SolicitudResponse;

import java.util.List;

public interface GestionSolicitudesUseCase {

    SolicitudResponse crearSolicitud(CrearSolicitudRequest request);

    List<SolicitudResponse> consultarHistorial(Long clienteId);

    SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request);

    SolicitudResponse cancelarSolicitud(Long id);
}

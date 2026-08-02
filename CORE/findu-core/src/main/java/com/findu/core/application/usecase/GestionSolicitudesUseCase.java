package com.findu.core.application.usecase;

import com.findu.core.dto.request.CrearSolicitudRequest;
import com.findu.core.dto.request.ModificarSolicitudRequest;
import com.findu.core.dto.response.SolicitudResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionSolicitudesUseCase {

    SolicitudResponse crearSolicitud(CrearSolicitudRequest request);

    Page<SolicitudResponse> consultarHistorial(Long clienteId, String estado, Pageable pageable);

    SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request);

    SolicitudResponse cancelarSolicitud(Long id);
}

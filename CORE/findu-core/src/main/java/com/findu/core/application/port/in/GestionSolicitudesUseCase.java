package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearSolicitudRequest;
import com.findu.core.application.dto.request.ModificarSolicitudRequest;
import com.findu.core.application.dto.response.SolicitudResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionSolicitudesUseCase {

    SolicitudResponse crearSolicitud(CrearSolicitudRequest request);

    Page<SolicitudResponse> consultarHistorial(Long clienteId, String estado, Pageable pageable);

    SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request);

    SolicitudResponse cancelarSolicitud(Long id);

    SolicitudResponse iniciarServicio(Long id);

    SolicitudResponse finalizarServicio(Long id);

    SolicitudResponse obtenerSolicitudPorId(Long id);
}

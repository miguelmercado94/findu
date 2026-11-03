package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearSolicitudRequest;
import com.findu.core.application.dto.request.IniciarServicioRequest;
import com.findu.core.application.dto.request.ModificarSolicitudRequest;
import com.findu.core.application.dto.response.SolicitudResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface GestionSolicitudesUseCase {

    SolicitudResponse crearSolicitud(CrearSolicitudRequest request);

    Page<SolicitudResponse> consultarHistorial(Long clienteId, String estado, Pageable pageable);

    SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request);

    SolicitudResponse cancelarSolicitud(Long id);

    SolicitudResponse iniciarServicio(Long id);

    SolicitudResponse iniciarServicio(Long id, IniciarServicioRequest request);

    SolicitudResponse finalizarServicio(Long id);

    SolicitudResponse finalizarServicio(Long id, BigDecimal latitud, BigDecimal longitud);

    SolicitudResponse reabrirSolicitudCliente(Long id, BigDecimal latitud, BigDecimal longitud);

    SolicitudResponse obtenerSolicitudPorId(Long id);

    SolicitudResponse obtenerSolicitudParaCliente(Long id, Long clienteId);

    String obtenerPinInicioParaCliente(Long id, Long clienteId);

    void enviarAlarmasUnaHoraAntes();
}

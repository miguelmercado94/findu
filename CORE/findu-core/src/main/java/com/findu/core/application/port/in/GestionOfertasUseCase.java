package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;

import java.util.List;

public interface GestionOfertasUseCase {

    OfertaResponse enviarOferta(CrearOfertaRequest request);

    List<OfertaResponse> listarOfertasPorSolicitud(Long solicitudId);

    OfertaResponse aceptarOferta(Long ofertaId);

    List<SolicitudResponse> obtenerSolicitudesDisponibles(Long proveedorId);
}

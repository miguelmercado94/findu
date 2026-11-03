package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearOfertaRequest;
import com.findu.core.application.dto.response.OfertaProveedorDetalleResponse;
import com.findu.core.application.dto.response.OfertaResponse;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

import java.util.List;

public interface GestionOfertasUseCase {

    OfertaResponse enviarOferta(CrearOfertaRequest request);

    OfertaResponse actualizarOferta(Long ofertaId, CrearOfertaRequest request);

    void retirarOferta(Long ofertaId, Long proveedorId);

    List<OfertaResponse> listarOfertasPorSolicitud(Long solicitudId);

    OfertaResponse aceptarOferta(Long ofertaId);

    List<SolicitudResponse> obtenerSolicitudesDisponibles(Long proveedorId);

    List<SolicitudResponse> obtenerMisSolicitudes(Long proveedorId, String estado);

    List<OfertaProveedorDetalleResponse> obtenerMisOfertasProveedor(Long proveedorId);

    boolean verificarSolapamientoHorario(Long proveedorId, SolicitudServicio solicitud, Oferta oferta);

    void validarNoSolapamientoHorarioProveedor(Long proveedorId, SolicitudServicio solicitud, Oferta oferta);
}

package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudAbierta implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "ABIERTA";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        solicitud.setEstadoSolicitud("EN_NEGOCIACION");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("No se puede aceptar oferta directamente desde estado ABIERTA sin ofertas previas.");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("No se puede iniciar servicio en estado ABIERTA.");
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("No se puede finalizar servicio en estado ABIERTA.");
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("CANCELADA");
    }
}

package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudCancelada implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "CANCELADA";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("La solicitud fue cancelada.");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("La solicitud fue cancelada.");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("La solicitud fue cancelada.");
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("La solicitud fue cancelada.");
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        // Ya está cancelada
    }
}

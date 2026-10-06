package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudFinalizada implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "FINALIZADA";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("La solicitud fue finalizada.");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("La solicitud fue finalizada.");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("La solicitud fue finalizada.");
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        // Ya está finalizada
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        throw new IllegalStateException("No se puede cancelar una solicitud finalizada.");
    }
}

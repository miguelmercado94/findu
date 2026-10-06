package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudAceptada implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "ACEPTADA";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("La solicitud ya aceptó una oferta y no recibe más propuestas.");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("Ya existe una oferta aceptada para esta solicitud.");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("EN_CURSO");
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("Primero se debe iniciar el servicio antes de finalizarlo.");
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("CANCELADA");
    }
}

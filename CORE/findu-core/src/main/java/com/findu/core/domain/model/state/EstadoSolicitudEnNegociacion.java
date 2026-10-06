package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudEnNegociacion implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "EN_NEGOCIACION";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        // Se mantiene en negociación al recibir más ofertas
        solicitud.setEstadoSolicitud("EN_NEGOCIACION");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        solicitud.setEstadoSolicitud("ACEPTADA");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("No se puede iniciar servicio sin antes aceptar una oferta.");
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        throw new IllegalStateException("No se puede finalizar servicio en negociación.");
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("CANCELADA");
    }
}

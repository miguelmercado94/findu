package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public class EstadoSolicitudEnCurso implements SolicitudEstadoState {

    @Override
    public String getNombreEstado() {
        return "EN_CURSO";
    }

    @Override
    public void recibirOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("El servicio ya está en curso.");
    }

    @Override
    public void aceptarOferta(SolicitudServicio solicitud, Oferta oferta) {
        throw new IllegalStateException("El servicio ya está en curso.");
    }

    @Override
    public void iniciarServicio(SolicitudServicio solicitud) {
        // Ya está en curso
    }

    @Override
    public void finalizarServicio(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("FINALIZADA");
    }

    @Override
    public void cancelarSolicitud(SolicitudServicio solicitud) {
        solicitud.setEstadoSolicitud("CANCELADA");
    }
}

package com.findu.core.domain.model.state;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.SolicitudServicio;

public interface SolicitudEstadoState {

    String getNombreEstado();

    void recibirOferta(SolicitudServicio solicitud, Oferta oferta);

    void aceptarOferta(SolicitudServicio solicitud, Oferta oferta);

    void iniciarServicio(SolicitudServicio solicitud);

    void finalizarServicio(SolicitudServicio solicitud);

    void cancelarSolicitud(SolicitudServicio solicitud);
}

package com.findu.core.domain.observer;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;

import java.util.List;

public interface SolicitudObserver {

    void onSolicitudCreada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresElegibles);

    void onOfertaRecibida(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor);

    void onOfertaAceptada(SolicitudServicio solicitud, Oferta ofertaAceptada, List<Oferta> ofertasRechazadas, PerfilProveedor proveedorAceptado, List<PerfilProveedor> proveedoresNotificados);

    void onSolicitudCancelada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresNotificados, List<Oferta> ofertasExistentes);

    void onSolicitudActualizada(SolicitudServicio solicitud, List<PerfilProveedor> nuevosProveedores, List<PerfilProveedor> proveedoresRemovidos);

    default void onSolicitudModificadaParaOferentes(
            SolicitudServicio solicitud,
            List<Oferta> ofertasPendientesReoferta,
            List<PerfilProveedor> proveedoresComoNuevaSolicitud,
            List<PerfilProveedor> proveedoresSinOfertaActualizados,
            List<PerfilProveedor> proveedoresRemovidos
    ) {
        onSolicitudActualizada(solicitud, proveedoresSinOfertaActualizados, proveedoresRemovidos);
    }

    default void onOfertaRetirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {}

    default void onOfertaExpirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {}

    default void onServicioIniciado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {}

    default void onServicioFinalizado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {}

    default void onAlarmaServicio1Hora(SolicitudServicio solicitud, Oferta ofertaAceptada, PerfilProveedor proveedor) {}
}

package com.findu.core.domain.observer;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SolicitudEventPublisher {

    private final List<SolicitudObserver> observers;

    public void notifySolicitudCreada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresElegibles) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onSolicitudCreada(solicitud, proveedoresElegibles);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyOfertaRecibida(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onOfertaRecibida(solicitud, oferta, cliente, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyOfertaAceptada(SolicitudServicio solicitud, Oferta ofertaAceptada, List<Oferta> ofertasRechazadas, PerfilProveedor proveedorAceptado, List<PerfilProveedor> proveedoresNotificados) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onOfertaAceptada(solicitud, ofertaAceptada, ofertasRechazadas, proveedorAceptado, proveedoresNotificados);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifySolicitudCancelada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresNotificados, List<Oferta> ofertasExistentes) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onSolicitudCancelada(solicitud, proveedoresNotificados, ofertasExistentes);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifySolicitudActualizada(SolicitudServicio solicitud, List<PerfilProveedor> nuevosProveedores, List<PerfilProveedor> proveedoresRemovidos) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onSolicitudActualizada(solicitud, nuevosProveedores, proveedoresRemovidos);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }
}


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

    public void notifySolicitudModificadaParaOferentes(
            SolicitudServicio solicitud,
            List<Oferta> ofertasPendientesReoferta,
            List<PerfilProveedor> proveedoresComoNuevaSolicitud,
            List<PerfilProveedor> proveedoresSinOfertaActualizados,
            List<PerfilProveedor> proveedoresRemovidos
    ) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onSolicitudModificadaParaOferentes(
                        solicitud,
                        ofertasPendientesReoferta,
                        proveedoresComoNuevaSolicitud,
                        proveedoresSinOfertaActualizados,
                        proveedoresRemovidos
                );
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyOfertaRetirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onOfertaRetirada(solicitud, oferta, cliente, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyOfertaExpirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onOfertaExpirada(solicitud, oferta, cliente, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyServicioIniciado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onServicioIniciado(solicitud, cliente, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyServicioFinalizado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onServicioFinalizado(solicitud, cliente, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }

    public void notifyAlarmaServicio1Hora(SolicitudServicio solicitud, Oferta ofertaAceptada, PerfilProveedor proveedor) {
        if (observers == null) return;
        for (SolicitudObserver observer : observers) {
            try {
                observer.onAlarmaServicio1Hora(solicitud, ofertaAceptada, proveedor);
            } catch (Exception e) {
                // Non-blocking
            }
        }
    }
}

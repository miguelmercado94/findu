package com.findu.core.domain.observer;

import com.findu.core.application.port.output.externalapi.NotificationPort;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.model.constants.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatcherNotificationObserver implements SolicitudObserver {

    private final NotificationPort notificationPort;

    @Override
    public void onSolicitudCreada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresElegibles) {
        if (proveedoresElegibles == null || proveedoresElegibles.isEmpty()) {
            log.info("No hay proveedores elegibles en línea para la solicitud ID {}", solicitud.getId());
            return;
        }

        log.info("Observer notificando a {} proveedores elegibles sobre solicitud ID {}", proveedoresElegibles.size(), solicitud.getId());

        for (PerfilProveedor p : proveedoresElegibles) {
            String recipient = "PROVEEDOR_" + p.getId();
            Map<String, String> params = new HashMap<>();
            params.put("solicitudId", String.valueOf(solicitud.getId()));
            params.put("detalles", solicitud.getDetalles() != null ? solicitud.getDetalles() : "");
            params.put("presupuestoMaximo", solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo().toString() : "0");
            params.put("esPresupuestoEstricto", String.valueOf(Boolean.TRUE.equals(solicitud.getEsPresupuestoEstricto())));
            params.put("tipoEvento", "NUEVA_SOLICITUD");

            notificationPort.send("PUSH", recipient, NotificationTemplates.SOLICITUD_CREADA, "es", params);
        }
    }

    @Override
    public void onOfertaRecibida(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        String recipient = cliente != null && cliente.getUsername() != null ? cliente.getUsername() : "CLIENTE_" + solicitud.getPerfilClienteId();
        String proveedorNombre = proveedor != null ? proveedor.getNombreCompleto() : "Proveedor FINDU";

        log.info("Observer notificando al cliente {} sobre nueva oferta de {}", recipient, proveedorNombre);

        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("ofertaId", String.valueOf(oferta.getId()));
        params.put("proveedorId", String.valueOf(oferta.getPerfilProveedorId()));
        params.put("proveedorNombre", proveedorNombre);
        params.put("valorPropuesto", oferta.getValorPropuesto() != null ? oferta.getValorPropuesto().toString() : "0");
        params.put("tiempoEstimado", oferta.getTiempoEstimado() != null ? oferta.getTiempoEstimado() : "");
        params.put("tipoEvento", "NUEVA_OFERTA");

        notificationPort.send("PUSH", recipient, NotificationTemplates.OFERTA_RECIBIDA, "es", params);
    }

    @Override
    public void onOfertaAceptada(SolicitudServicio solicitud, Oferta ofertaAceptada, List<Oferta> ofertasRechazadas, PerfilProveedor proveedorAceptado, List<PerfilProveedor> proveedoresNotificados) {
        Long ganadorId = proveedorAceptado != null ? proveedorAceptado.getId() : (ofertaAceptada != null ? ofertaAceptada.getPerfilProveedorId() : null);

        if (ganadorId != null) {
            String recipientGanador = "PROVEEDOR_" + ganadorId;
            log.info("Observer notificando al proveedor ganador ID {}", ganadorId);

            Map<String, String> paramsGanador = new HashMap<>();
            paramsGanador.put("solicitudId", String.valueOf(solicitud.getId()));
            paramsGanador.put("ofertaId", String.valueOf(ofertaAceptada.getId()));
            paramsGanador.put("valorPropuesto", ofertaAceptada.getValorPropuesto() != null ? ofertaAceptada.getValorPropuesto().toString() : "0");
            paramsGanador.put("tipoEvento", "OFERTA_ACEPTADA");

            notificationPort.send("PUSH", recipientGanador, NotificationTemplates.OFERTA_ACEPTADA, "es", paramsGanador);
        }

        java.util.Set<String> otrosRecipients = new java.util.HashSet<>();
        if (proveedoresNotificados != null) {
            for (PerfilProveedor p : proveedoresNotificados) {
                if (ganadorId != null && p.getId().equals(ganadorId)) continue;
                otrosRecipients.add("PROVEEDOR_" + p.getId());
            }
        }
        if (ofertasRechazadas != null) {
            for (Oferta o : ofertasRechazadas) {
                if (ganadorId != null && o.getPerfilProveedorId().equals(ganadorId)) continue;
                otrosRecipients.add("PROVEEDOR_" + o.getPerfilProveedorId());
            }
        }

        log.info("Disipando evento SOLICITUD_CERRADA a {} proveedores no elegidos", otrosRecipients.size());

        for (String recipientOtros : otrosRecipients) {
            Map<String, String> paramsDisipacion = new HashMap<>();
            paramsDisipacion.put("solicitudId", String.valueOf(solicitud.getId()));
            paramsDisipacion.put("motivo", "OFERTA_OTRO_PROVEEDOR_ACEPTADA");
            paramsDisipacion.put("tipoEvento", "SOLICITUD_CERRADA");

            notificationPort.send("PUSH", recipientOtros, "SOLICITUD_CERRADA", "es", paramsDisipacion);
        }
    }

    @Override
    public void onSolicitudCancelada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresNotificados, List<Oferta> ofertasExistentes) {
        java.util.Set<String> recipients = new java.util.HashSet<>();
        if (proveedoresNotificados != null) {
            for (PerfilProveedor p : proveedoresNotificados) {
                recipients.add("PROVEEDOR_" + p.getId());
            }
        }
        if (ofertasExistentes != null) {
            for (Oferta o : ofertasExistentes) {
                recipients.add("PROVEEDOR_" + o.getPerfilProveedorId());
            }
        }

        log.info("Observer disipando evento SOLICITUD_CERRADA a {} proveedores por cancelación de solicitud ID {}", recipients.size(), solicitud.getId());

        for (String recipient : recipients) {
            Map<String, String> params = new HashMap<>();
            params.put("solicitudId", String.valueOf(solicitud.getId()));
            params.put("motivo", "SOLICITUD_CANCELADA_POR_CLIENTE");
            params.put("tipoEvento", "SOLICITUD_CERRADA");

            notificationPort.send("PUSH", recipient, "SOLICITUD_CERRADA", "es", params);
        }
    }

    @Override
    public void onSolicitudActualizada(SolicitudServicio solicitud, List<PerfilProveedor> nuevosProveedores, List<PerfilProveedor> proveedoresRemovidos) {
        if (nuevosProveedores != null) {
            log.info("Observer notificando SOLICITUD_ACTUALIZADA a {} proveedores para solicitud ID {}", nuevosProveedores.size(), solicitud.getId());
            for (PerfilProveedor p : nuevosProveedores) {
                String recipient = "PROVEEDOR_" + p.getId();
                Map<String, String> params = new HashMap<>();
                params.put("solicitudId", String.valueOf(solicitud.getId()));
                params.put("detalles", solicitud.getDetalles() != null ? solicitud.getDetalles() : "");
                params.put("presupuestoMaximo", solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo().toString() : "0");
                params.put("esPresupuestoEstricto", String.valueOf(Boolean.TRUE.equals(solicitud.getEsPresupuestoEstricto())));
                params.put("direccionId", solicitud.getDireccionId() != null ? String.valueOf(solicitud.getDireccionId()) : "");
                params.put("fechaProgramada", solicitud.getFechaProgramada() != null ? solicitud.getFechaProgramada().toString() : "");
                params.put("tipoEvento", NotificationTemplates.SOLICITUD_ACTUALIZADA);

                notificationPort.send("PUSH", recipient, NotificationTemplates.SOLICITUD_ACTUALIZADA, "es", params);
            }
        }

        if (proveedoresRemovidos != null && !proveedoresRemovidos.isEmpty()) {
            log.info("Observer disipando SOLICITUD_CERRADA a {} proveedores fuera de cobertura para solicitud ID {}", proveedoresRemovidos.size(), solicitud.getId());
            for (PerfilProveedor p : proveedoresRemovidos) {
                String recipient = "PROVEEDOR_" + p.getId();
                Map<String, String> params = new HashMap<>();
                params.put("solicitudId", String.valueOf(solicitud.getId()));
                params.put("motivo", "CAMBIO_COBERTURA");
                params.put("tipoEvento", "SOLICITUD_CERRADA");

                notificationPort.send("PUSH", recipient, "SOLICITUD_CERRADA", "es", params);
            }
        }
    }
}


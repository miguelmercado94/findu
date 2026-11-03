package com.findu.core.domain.observer;

import com.findu.core.application.port.out.externalapi.NotificationPort;
import com.findu.core.application.port.out.persistence.CategoriaRepositoryPort;
import com.findu.core.application.port.out.persistence.MunicipioRepositoryPort;
import com.findu.core.application.port.out.persistence.ServicioRepositoryPort;
import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.domain.enums.NotificationTemplates;
import com.findu.core.domain.model.*;
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
    private final ServicioRepositoryPort servicioPort;
    private final CategoriaRepositoryPort categoriaPort;
    private final DireccionService direccionService;
    private final MunicipioRepositoryPort municipioPort;
    private final PerfilClienteService perfilClienteService;
    private final PerfilProveedorService proveedorService;

    private void sendToProvider(PerfilProveedor p, String eventType, String template, Map<String, String> params) {
        if (p == null || p.getId() == null) return;
        notificationPort.send(eventType, "PROVEEDOR_" + p.getId(), template, "es", params);
        if (p.getAuthUserId() != null && !p.getAuthUserId().equals(p.getId())) {
            notificationPort.send(eventType, "PROVEEDOR_" + p.getAuthUserId(), template, "es", params);
        }
    }

    private void sendToProviderId(Long proveedorId, String eventType, String template, Map<String, String> params) {
        if (proveedorId == null) return;
        notificationPort.send(eventType, "PROVEEDOR_" + proveedorId, template, "es", params);
        if (proveedorService != null) {
            proveedorService.findById(proveedorId).ifPresent(p -> {
                if (p.getAuthUserId() != null && !p.getAuthUserId().equals(proveedorId)) {
                    notificationPort.send(eventType, "PROVEEDOR_" + p.getAuthUserId(), template, "es", params);
                }
            });
        }
    }

    private Map<String, String> buildBaseSolicitudParams(SolicitudServicio solicitud) {
        Servicio servicio = solicitud.getServicioId() != null
                ? servicioPort.findById(solicitud.getServicioId()).orElse(null)
                : null;
        Categoria categoria = (servicio != null && servicio.getCategoriaId() != null)
                ? categoriaPort.findById(servicio.getCategoriaId()).orElse(null)
                : null;

        Direccion direccion = solicitud.getDireccionId() != null
                ? direccionService.findById(solicitud.getDireccionId()).orElse(null)
                : null;
        Municipio municipio = (direccion != null && direccion.getMunicipioId() != null)
                ? municipioPort.findById(direccion.getMunicipioId()).orElse(null)
                : null;

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("servicioId", solicitud.getServicioId() != null ? String.valueOf(solicitud.getServicioId()) : "");
        params.put("servicioNombre", servicio != null ? servicio.getNombre() : "Servicio General");
        params.put("categoria", categoria != null ? categoria.getNombre() : "Servicios");
        params.put("detalles", solicitud.getDetalles() != null ? solicitud.getDetalles() : "");
        params.put("presupuestoMaximo", solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo().toString() : "0");
        params.put("esPresupuestoEstricto", String.valueOf(Boolean.TRUE.equals(solicitud.getEsPresupuestoEstricto())));
        params.put("direccionId", solicitud.getDireccionId() != null ? String.valueOf(solicitud.getDireccionId()) : "");
        params.put("direccionTexto", direccion != null ? direccion.getDireccionTexto() : "");
        params.put("municipioNombre", municipio != null ? municipio.getNombre() : "");
        params.put("codigoDane", municipio != null ? municipio.getCodigoDane() : "");
        params.put("fechaProgramada", solicitud.getFechaProgramada() != null ? solicitud.getFechaProgramada().toString() : "");
        params.put("distanciaKm", "2.0");
        params.put("clienteNombre", cliente != null ? cliente.getNombreCompleto() : (solicitud.getNombreContacto() != null ? solicitud.getNombreContacto() : "Cliente FindU"));
        params.put("clienteCalificacion", cliente != null && cliente.getCalificacionPromedio() != null ? cliente.getCalificacionPromedio().toString() : "5.0");
        params.put("estado", solicitud.getEstadoSolicitud() != null ? solicitud.getEstadoSolicitud() : "ABIERTA");
        return params;
    }

    @Override
    public void onSolicitudCreada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresElegibles) {
        if (proveedoresElegibles == null || proveedoresElegibles.isEmpty()) {
            log.info("No hay proveedores elegibles en línea para la solicitud ID {}", solicitud.getId());
            return;
        }

        log.info("Observer notificando a {} proveedores elegibles sobre solicitud ID {}", proveedoresElegibles.size(), solicitud.getId());
        Map<String, String> baseParams = buildBaseSolicitudParams(solicitud);

        for (PerfilProveedor p : proveedoresElegibles) {
            Map<String, String> params = new HashMap<>(baseParams);
            params.put("tipoEvento", "NUEVA_SOLICITUD");
            sendToProvider(p, "SERVICE_UPDATE", NotificationTemplates.SOLICITUD_CREADA, params);
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
        params.put("horaInicioPropuesta", oferta.getHoraInicioPropuesta() != null ? oferta.getHoraInicioPropuesta().toString() : "");
        params.put("tiempoEstimado", oferta.getTiempoEstimado() != null ? oferta.getTiempoEstimado() : "");
        params.put("tipoEvento", "NUEVA_OFERTA");

        notificationPort.send("OFFER_RECEIVED", recipient, NotificationTemplates.OFERTA_RECIBIDA, "es", params);
    }

    @Override
    public void onOfertaAceptada(SolicitudServicio solicitud, Oferta ofertaAceptada, List<Oferta> ofertasRechazadas, PerfilProveedor proveedorAceptado, List<PerfilProveedor> proveedoresNotificados) {
        Long ganadorId = proveedorAceptado != null ? proveedorAceptado.getId() : (ofertaAceptada != null ? ofertaAceptada.getPerfilProveedorId() : null);

        if (ganadorId != null) {
            log.info("Observer notificando al proveedor ganador ID {}", ganadorId);

            Map<String, String> paramsGanador = new HashMap<>();
            paramsGanador.put("solicitudId", String.valueOf(solicitud.getId()));
            paramsGanador.put("ofertaId", String.valueOf(ofertaAceptada.getId()));
            paramsGanador.put("valorPropuesto", ofertaAceptada.getValorPropuesto() != null ? ofertaAceptada.getValorPropuesto().toString() : "0");
            paramsGanador.put("horaInicioPropuesta", ofertaAceptada.getHoraInicioPropuesta() != null ? ofertaAceptada.getHoraInicioPropuesta().toString() : "");
            paramsGanador.put("fechaProgramada", solicitud.getFechaProgramada() != null ? solicitud.getFechaProgramada().toString() : "");
            paramsGanador.put("tipoEvento", "OFERTA_ACEPTADA");

            if (proveedorAceptado != null) {
                sendToProvider(proveedorAceptado, "SERVICE_UPDATE", NotificationTemplates.OFERTA_ACEPTADA, paramsGanador);
            } else {
                sendToProviderId(ganadorId, "SERVICE_UPDATE", NotificationTemplates.OFERTA_ACEPTADA, paramsGanador);
            }
        }

        java.util.Set<Long> otrosProveedorIds = new java.util.HashSet<>();
        if (proveedoresNotificados != null) {
            for (PerfilProveedor p : proveedoresNotificados) {
                if (ganadorId != null && p.getId().equals(ganadorId)) continue;
                otrosProveedorIds.add(p.getId());
            }
        }
        if (ofertasRechazadas != null) {
            for (Oferta o : ofertasRechazadas) {
                if (ganadorId != null && o.getPerfilProveedorId().equals(ganadorId)) continue;
                otrosProveedorIds.add(o.getPerfilProveedorId());
            }
        }

        log.info("Disipando evento SOLICITUD_CERRADA a {} proveedores no elegidos", otrosProveedorIds.size());

        for (Long pId : otrosProveedorIds) {
            Map<String, String> paramsDisipacion = new HashMap<>();
            paramsDisipacion.put("solicitudId", String.valueOf(solicitud.getId()));
            paramsDisipacion.put("motivo", "OFERTA_OTRO_PROVEEDOR_ACEPTADA");
            paramsDisipacion.put("tipoEvento", "SOLICITUD_CERRADA");

            sendToProviderId(pId, "SERVICE_UPDATE", "SOLICITUD_CERRADA", paramsDisipacion);
        }
    }

    @Override
    public void onSolicitudCancelada(SolicitudServicio solicitud, List<PerfilProveedor> proveedoresNotificados, List<Oferta> ofertasExistentes) {
        java.util.Set<Long> providerIds = new java.util.HashSet<>();
        if (proveedoresNotificados != null) {
            for (PerfilProveedor p : proveedoresNotificados) {
                providerIds.add(p.getId());
            }
        }
        if (ofertasExistentes != null) {
            for (Oferta o : ofertasExistentes) {
                providerIds.add(o.getPerfilProveedorId());
            }
        }

        log.info("Observer disipando evento SOLICITUD_CERRADA a {} proveedores por cancelación de solicitud ID {}", providerIds.size(), solicitud.getId());

        for (Long pId : providerIds) {
            Map<String, String> params = new HashMap<>();
            params.put("solicitudId", String.valueOf(solicitud.getId()));
            params.put("motivo", "SOLICITUD_CANCELADA_POR_CLIENTE");
            params.put("tipoEvento", "SOLICITUD_CERRADA");

            sendToProviderId(pId, "SERVICE_UPDATE", "SOLICITUD_CERRADA", params);
        }
    }

    @Override
    public void onSolicitudActualizada(SolicitudServicio solicitud, List<PerfilProveedor> nuevosProveedores, List<PerfilProveedor> proveedoresRemovidos) {
        Map<String, String> baseParams = buildBaseSolicitudParams(solicitud);
        if (nuevosProveedores != null) {
            log.info("Observer notificando SOLICITUD_ACTUALIZADA a {} proveedores para solicitud ID {}", nuevosProveedores.size(), solicitud.getId());
            for (PerfilProveedor p : nuevosProveedores) {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("tipoEvento", NotificationTemplates.SOLICITUD_ACTUALIZADA);
                sendToProvider(p, "SERVICE_UPDATE", NotificationTemplates.SOLICITUD_ACTUALIZADA, params);
            }
        }

        if (proveedoresRemovidos != null && !proveedoresRemovidos.isEmpty()) {
            log.info("Observer disipando SOLICITUD_CERRADA a {} proveedores fuera de cobertura para solicitud ID {}", proveedoresRemovidos.size(), solicitud.getId());
            for (PerfilProveedor p : proveedoresRemovidos) {
                Map<String, String> params = new HashMap<>();
                params.put("solicitudId", String.valueOf(solicitud.getId()));
                params.put("motivo", "CAMBIO_COBERTURA");
                params.put("tipoEvento", "SOLICITUD_CERRADA");

                sendToProvider(p, "SERVICE_UPDATE", "SOLICITUD_CERRADA", params);
            }
        }
    }

    @Override
    public void onSolicitudModificadaParaOferentes(
            SolicitudServicio solicitud,
            List<Oferta> ofertasPendientesReoferta,
            List<PerfilProveedor> proveedoresComoNuevaSolicitud,
            List<PerfilProveedor> proveedoresSinOfertaActualizados,
            List<PerfilProveedor> proveedoresRemovidos
    ) {
        Map<String, String> baseParams = buildBaseSolicitudParams(solicitud);

        // 1. Proveedores que tenían oferta vigente (< 10 min): reciben SOLICITUD_ACTUALIZADA en su pestaña "Ofertas" (parpadeando)
        if (ofertasPendientesReoferta != null && !ofertasPendientesReoferta.isEmpty()) {
            log.info("Observer notificando cambio en solicitud ID {} a {} proveedores con oferta activa (PENDIENTE_REOFERTA)",
                    solicitud.getId(), ofertasPendientesReoferta.size());
            for (Oferta o : ofertasPendientesReoferta) {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("ofertaId", String.valueOf(o.getId()));
                params.put("estadoOferta", "PENDIENTE_REOFERTA");
                params.put("solicitudModificada", "true");
                String campos = o.getCamposModificados() != null ? o.getCamposModificados() : "";
                params.put("camposModificados", campos);
                params.put("cambioFechaHora", String.valueOf(campos.contains("fechaProgramada")));
                params.put("solapamientoHorario", String.valueOf(Boolean.TRUE.equals(o.getSolapamientoHorario())));
                params.put("valoresAnteriores", o.getValoresAnteriores() != null ? o.getValoresAnteriores() : "{}");
                params.put("tipoEvento", NotificationTemplates.SOLICITUD_ACTUALIZADA);

                sendToProviderId(o.getPerfilProveedorId(), "SERVICE_UPDATE", NotificationTemplates.SOLICITUD_ACTUALIZADA, params);
            }
        }

        // 2. Proveedores cuya oferta había expirado (>= 10 min): al editar el cliente la solicitud, les aparece como NUEVA_SOLICITUD en Mercado en Vivo
        if (proveedoresComoNuevaSolicitud != null && !proveedoresComoNuevaSolicitud.isEmpty()) {
            log.info("Observer enviando NUEVA_SOLICITUD a {} proveedores cuya oferta previa había expirado (> 10 min) para solicitud ID {}",
                    proveedoresComoNuevaSolicitud.size(), solicitud.getId());
            for (PerfilProveedor p : proveedoresComoNuevaSolicitud) {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("tipoEvento", "NUEVA_SOLICITUD");
                params.put("reaperturaPorCambioCliente", "true");
                sendToProvider(p, "SERVICE_UPDATE", NotificationTemplates.SOLICITUD_CREADA, params);
            }
        }

        // 3. Proveedores elegibles que aún no habían ofertado y siguen en cobertura
        if (proveedoresSinOfertaActualizados != null && !proveedoresSinOfertaActualizados.isEmpty()) {
            for (PerfilProveedor p : proveedoresSinOfertaActualizados) {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("tipoEvento", NotificationTemplates.SOLICITUD_ACTUALIZADA);
                sendToProvider(p, "SERVICE_UPDATE", NotificationTemplates.SOLICITUD_ACTUALIZADA, params);
            }
        }

        // 4. Proveedores que quedaron fuera de cobertura tras el cambio de dirección
        if (proveedoresRemovidos != null && !proveedoresRemovidos.isEmpty()) {
            for (PerfilProveedor p : proveedoresRemovidos) {
                Map<String, String> params = new HashMap<>();
                params.put("solicitudId", String.valueOf(solicitud.getId()));
                params.put("motivo", "CAMBIO_COBERTURA");
                params.put("tipoEvento", "SOLICITUD_CERRADA");
                sendToProvider(p, "SERVICE_UPDATE", "SOLICITUD_CERRADA", params);
            }
        }
    }

    @Override
    public void onOfertaRetirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("ofertaId", String.valueOf(oferta.getId()));
        params.put("proveedorId", String.valueOf(oferta.getPerfilProveedorId()));
        params.put("estadoOferta", "RETIRADA");
        params.put("tipoEvento", "OFERTA_RETIRADA");

        sendToProviderId(oferta.getPerfilProveedorId(), "SERVICE_UPDATE", "OFERTA_RETIRADA", params);

        if (cliente != null) {
            String recipient = cliente.getUsername() != null ? cliente.getUsername() : "CLIENTE_" + solicitud.getPerfilClienteId();
            notificationPort.send("SERVICE_UPDATE", recipient, "OFERTA_RETIRADA", "es", params);
        }
    }

    @Override
    public void onOfertaExpirada(SolicitudServicio solicitud, Oferta oferta, PerfilCliente cliente, PerfilProveedor proveedor) {
        log.info("Observer notificando OFERTA_EXPIRADA (10 min vencidos) para oferta ID {} en solicitud ID {}",
                oferta.getId(), solicitud.getId());

        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("ofertaId", String.valueOf(oferta.getId()));
        params.put("proveedorId", String.valueOf(oferta.getPerfilProveedorId()));
        params.put("estadoOferta", "EXPIRADA");
        params.put("motivo", "EXPIRACION_10_MIN");
        params.put("tipoEvento", "OFERTA_EXPIRADA");

        sendToProviderId(oferta.getPerfilProveedorId(), "SERVICE_UPDATE", "OFERTA_EXPIRADA", params);

        if (cliente != null) {
            String recipient = cliente.getUsername() != null ? cliente.getUsername() : "CLIENTE_" + solicitud.getPerfilClienteId();
            notificationPort.send("SERVICE_UPDATE", recipient, "OFERTA_EXPIRADA", "es", params);
        }
    }

    @Override
    public void onServicioIniciado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {
        log.info("Observer notificando SERVICIO_EN_CURSO para solicitud ID {}", solicitud.getId());

        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("estado", "EN_CURSO");
        params.put("tipoEvento", "SERVICIO_EN_CURSO");

        if (proveedor != null) {
            sendToProvider(proveedor, "SERVICE_UPDATE", "SERVICIO_EN_CURSO", params);
        }
        if (cliente != null) {
            String clientRecipient = cliente.getUsername() != null ? cliente.getUsername() : "CLIENTE_" + cliente.getId();
            notificationPort.send("SERVICE_UPDATE", clientRecipient, "SERVICIO_EN_CURSO", "es", params);
        }
    }

    @Override
    public void onServicioFinalizado(SolicitudServicio solicitud, PerfilCliente cliente, PerfilProveedor proveedor) {
        log.info("Observer notificando SERVICIO_FINALIZADO para solicitud ID {}", solicitud.getId());

        Map<String, String> params = new HashMap<>();
        params.put("solicitudId", String.valueOf(solicitud.getId()));
        params.put("estado", "FINALIZADA");
        params.put("tipoEvento", "SERVICIO_FINALIZADO");

        if (proveedor != null) {
            sendToProvider(proveedor, "SERVICE_UPDATE", "SERVICIO_FINALIZADO", params);
        }
        if (cliente != null) {
            String clientRecipient = cliente.getUsername() != null ? cliente.getUsername() : "CLIENTE_" + cliente.getId();
            notificationPort.send("SERVICE_UPDATE", clientRecipient, "SERVICIO_FINALIZADO", "es", params);
        }
    }

    @Override
    public void onAlarmaServicio1Hora(SolicitudServicio solicitud, Oferta ofertaAceptada, PerfilProveedor proveedor) {
        log.info("Observer enviando ALARMA_SERVICIO_1H (1 hora antes de iniciar) para solicitud ID {}", solicitud.getId());

        Map<String, String> params = new HashMap<>(buildBaseSolicitudParams(solicitud));
        if (ofertaAceptada != null && ofertaAceptada.getId() != null) {
            params.put("ofertaId", String.valueOf(ofertaAceptada.getId()));
        }
        params.put("minutosParaIniciar", "60");
        params.put("tipoEvento", NotificationTemplates.ALARMA_SERVICIO_1H);

        if (proveedor != null) {
            sendToProvider(proveedor, "SERVICE_UPDATE", NotificationTemplates.ALARMA_SERVICIO_1H, params);
        } else if (ofertaAceptada != null && ofertaAceptada.getPerfilProveedorId() != null) {
            sendToProviderId(ofertaAceptada.getPerfilProveedorId(), "SERVICE_UPDATE", NotificationTemplates.ALARMA_SERVICIO_1H, params);
        }
    }
}

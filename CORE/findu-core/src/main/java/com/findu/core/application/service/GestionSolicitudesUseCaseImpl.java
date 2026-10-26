package com.findu.core.application.service;

import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.OfertaService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.port.in.GestionSolicitudesUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import com.findu.core.application.dto.request.CrearSolicitudRequest;
import com.findu.core.application.dto.request.ModificarSolicitudRequest;
import com.findu.core.application.dto.response.SolicitudResponse;
import com.findu.core.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionSolicitudesUseCaseImpl implements GestionSolicitudesUseCase {

    private static final Set<String> ESTADOS_MODIFICABLES = Set.of("ABIERTA", "EN_NEGOCIACION");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("ABIERTA", "EN_NEGOCIACION", "PROGRAMADA");

    private final SolicitudServicioService solicitudService;
    private final PerfilProveedorService proveedorService;
    private final PerfilClienteService perfilClienteService;
    private final DireccionService direccionService;
    private final OfertaService ofertaService;
    private final SolicitudEventPublisher eventPublisher;
    private final com.findu.core.application.port.out.persistence.ServicioRepositoryPort servicioPort;
    private final com.findu.core.application.port.out.persistence.CategoriaRepositoryPort categoriaPort;
    private final com.findu.core.application.port.out.persistence.MunicipioRepositoryPort municipioPort;
    private final com.findu.core.infrastructure.adapter.out.externalapi.TransactionServiceClient transactionServiceClient;

    @Override
    public SolicitudResponse crearSolicitud(CrearSolicitudRequest request) {
        SolicitudServicio solicitud = SolicitudServicio.builder()
                .perfilClienteId(request.perfilClienteId())
                .servicioId(request.servicioId())
                .direccionId(request.direccionId())
                .fechaProgramada(request.fechaProgramada())
                .nombreContacto(request.nombreContacto())
                .telefonoContacto(request.telefonoContacto())
                .prioridad(request.prioridad() != null ? request.prioridad() : 3)
                .presupuestoMaximo(request.presupuestoMaximo())
                .esPresupuestoEstricto(request.esPresupuestoEstricto() != null ? request.esPresupuestoEstricto() : false)
                .cantidadEstimada(request.cantidadEstimada() != null ? request.cantidadEstimada() : 1)
                .detalles(request.detalles())
                .fotos(request.fotos())
                .estadoSolicitud("ABIERTA")
                .build();

        SolicitudServicio saved = solicitudService.save(solicitud);

        // Filter online providers matching coverage area & specialty
        Long municipioId = null;
        if (request.direccionId() != null) {
            municipioId = direccionService.findById(request.direccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }

        List<PerfilProveedor> proveedoresElegibles = proveedorService.findEligibleProviders(request.servicioId(), municipioId);
        eventPublisher.notifySolicitudCreada(saved, proveedoresElegibles);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudResponse> consultarHistorial(Long clienteId, String estado, Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return solicitudService.findByClienteIdAndEstado(clienteId, estado.toUpperCase(), pageable)
                    .map(this::toResponse);
        }
        return solicitudService.findByClienteId(clienteId, pageable)
                .map(this::toResponse);
    }

    @Override
    public SolicitudResponse modificarSolicitud(Long id, ModificarSolicitudRequest request) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        if (!ESTADOS_MODIFICABLES.contains(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("No se puede modificar una solicitud en estado: " + solicitud.getEstadoSolicitud());
        }

        Long oldDireccionId = solicitud.getDireccionId();

        if (request.direccionId() != null) solicitud.setDireccionId(request.direccionId());
        if (request.fechaProgramada() != null) solicitud.setFechaProgramada(request.fechaProgramada());
        if (request.nombreContacto() != null) solicitud.setNombreContacto(request.nombreContacto());
        if (request.telefonoContacto() != null) solicitud.setTelefonoContacto(request.telefonoContacto());
        if (request.prioridad() != null) solicitud.setPrioridad(request.prioridad());
        solicitud.setPresupuestoMaximo(request.presupuestoMaximo());
        if (request.esPresupuestoEstricto() != null) solicitud.setEsPresupuestoEstricto(request.esPresupuestoEstricto());
        if (request.detalles() != null) solicitud.setDetalles(request.detalles());
        if (request.fotos() != null) solicitud.setFotos(request.fotos());

        SolicitudServicio updated = solicitudService.save(solicitud);

        // Compute coverage changes and notify observers
        Long oldMunicipioId = null;
        if (oldDireccionId != null) {
            oldMunicipioId = direccionService.findById(oldDireccionId)
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        Long newMunicipioId = null;
        if (updated.getDireccionId() != null) {
            newMunicipioId = direccionService.findById(updated.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }

        List<PerfilProveedor> oldEligible = proveedorService.findEligibleProviders(updated.getServicioId(), oldMunicipioId);
        List<PerfilProveedor> newEligible = proveedorService.findEligibleProviders(updated.getServicioId(), newMunicipioId);

        List<PerfilProveedor> nuevosProveedores = newEligible;
        java.util.Set<Long> newEligibleIds = newEligible.stream().map(PerfilProveedor::getId).collect(java.util.stream.Collectors.toSet());
        List<PerfilProveedor> proveedoresRemovidos = oldEligible.stream()
                .filter(p -> !newEligibleIds.contains(p.getId()))
                .toList();

        eventPublisher.notifySolicitudActualizada(updated, nuevosProveedores, proveedoresRemovidos);

        return toResponse(updated);
    }

    @Override
    public SolicitudResponse cancelarSolicitud(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        if (!ESTADOS_CANCELABLES.contains(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("No se puede cancelar una solicitud en estado: " + solicitud.getEstadoSolicitud());
        }

        if ("PROGRAMADA".equals(solicitud.getEstadoSolicitud()) &&
                solicitud.getFechaProgramada() != null &&
                solicitud.getFechaProgramada().isBefore(LocalDateTime.now().plusHours(8))) {
            solicitud.setEstadoSolicitud("CANCELADA_CON_PENALIDAD");
        } else {
            solicitud.setEstadoSolicitud("CANCELADA_SIN_PENALIDAD");
        }

        SolicitudServicio updated = solicitudService.save(solicitud);

        // Notify observers to dissipate SOLICITUD_CERRADA event via WebSocket to providers
        Long municipioId = null;
        if (updated.getDireccionId() != null) {
            municipioId = direccionService.findById(updated.getDireccionId())
                    .map(Direccion::getMunicipioId)
                    .orElse(null);
        }
        List<PerfilProveedor> proveedoresNotificados = proveedorService.findEligibleProviders(updated.getServicioId(), municipioId);
        List<Oferta> ofertasExistentes = ofertaService.findBySolicitudId(updated.getId());

        eventPublisher.notifySolicitudCancelada(updated, proveedoresNotificados, ofertasExistentes);

        return toResponse(updated);
    }

    @Override
    public SolicitudResponse iniciarServicio(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        solicitud.iniciarServicio();
        SolicitudServicio updated = solicitudService.save(solicitud);

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        List<Oferta> ofertas = ofertaService.findBySolicitudId(id);
        Oferta aceptada = ofertas.stream()
                .filter(o -> "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                .findFirst()
                .orElse(null);

        PerfilProveedor proveedor = (aceptada != null && aceptada.getPerfilProveedorId() != null)
                ? proveedorService.findById(aceptada.getPerfilProveedorId()).orElse(null)
                : null;

        eventPublisher.notifyServicioIniciado(updated, cliente, proveedor);

        return toResponse(updated);
    }

    @Override
    public SolicitudResponse finalizarServicio(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        solicitud.finalizarServicio();
        SolicitudServicio updated = solicitudService.save(solicitud);

        List<Oferta> ofertas = ofertaService.findBySolicitudId(id);
        Oferta aceptada = ofertas.stream()
                .filter(o -> "ACEPTADA".equalsIgnoreCase(o.getEstadoOferta()))
                .findFirst()
                .orElse(null);

        Long providerId = aceptada != null ? aceptada.getPerfilProveedorId() : 1L;
        java.math.BigDecimal amount = (aceptada != null && aceptada.getValorPropuesto() != null)
                ? aceptada.getValorPropuesto()
                : (solicitud.getPresupuestoMaximo() != null ? solicitud.getPresupuestoMaximo() : java.math.BigDecimal.valueOf(100000));

        if (transactionServiceClient != null) {
            transactionServiceClient.registrarCreditoServicio(updated, providerId, amount);
        }

        PerfilCliente cliente = solicitud.getPerfilClienteId() != null
                ? perfilClienteService.findById(solicitud.getPerfilClienteId()).orElse(null)
                : null;

        PerfilProveedor proveedor = proveedorService.findById(providerId).orElse(null);

        eventPublisher.notifyServicioFinalizado(updated, cliente, proveedor);

        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse obtenerSolicitudPorId(Long id) {
        SolicitudServicio solicitud = solicitudService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));
        return toResponse(solicitud);
    }

    private SolicitudResponse toResponse(SolicitudServicio s) {
        String servicioNombre = null;
        String categoriaNombre = null;
        if (s.getServicioId() != null) {
            var servOpt = servicioPort.findById(s.getServicioId());
            if (servOpt.isPresent()) {
                var serv = servOpt.get();
                servicioNombre = serv.getNombre();
                if (serv.getCategoriaId() != null) {
                    categoriaNombre = categoriaPort.findById(serv.getCategoriaId())
                            .map(com.findu.core.domain.model.Categoria::getNombre)
                            .orElse(null);
                }
            }
        }

        String direccionTexto = null;
        String municipioNombre = null;
        String codigoDane = null;
        if (s.getDireccionId() != null) {
            var dirOpt = direccionService.findById(s.getDireccionId());
            if (dirOpt.isPresent()) {
                var dir = dirOpt.get();
                direccionTexto = dir.getDireccionTexto();
                if (dir.getMunicipioId() != null) {
                    var munOpt = municipioPort.findById(dir.getMunicipioId());
                    if (munOpt.isPresent()) {
                        municipioNombre = munOpt.get().getNombre();
                        codigoDane = munOpt.get().getCodigoDane();
                    }
                }
            }
        }

        String nombreCliente = s.getNombreContacto();
        java.math.BigDecimal clienteCalificacion = java.math.BigDecimal.valueOf(5.0);
        if (s.getPerfilClienteId() != null) {
            var cliOpt = perfilClienteService.findById(s.getPerfilClienteId());
            if (cliOpt.isPresent()) {
                if (nombreCliente == null || nombreCliente.isBlank()) {
                    nombreCliente = cliOpt.get().getNombreCompleto();
                }
                if (cliOpt.get().getCalificacionPromedio() != null) {
                    clienteCalificacion = cliOpt.get().getCalificacionPromedio();
                }
            }
        }

        java.math.BigDecimal tarifaAcordada = null;
        List<Oferta> ofertas = ofertaService.findBySolicitudId(s.getId());
        for (Oferta o : ofertas) {
            if ("ACEPTADA".equalsIgnoreCase(o.getEstadoOferta())) {
                tarifaAcordada = o.getValorPropuesto();
                break;
            }
        }

        return new SolicitudResponse(
                s.getId(),
                servicioNombre,
                categoriaNombre,
                direccionTexto,
                municipioNombre,
                codigoDane,
                2.0,
                s.getFechaProgramada(),
                nombreCliente != null ? nombreCliente : "Cliente FindU",
                s.getTelefonoContacto(),
                clienteCalificacion,
                s.getPrioridad(),
                s.getPresupuestoMaximo(),
                s.getEsPresupuestoEstricto(),
                tarifaAcordada,
                s.getDetalles(),
                s.getFotos(),
                s.getEstadoSolicitud(),
                null,
                null
        );
    }
}



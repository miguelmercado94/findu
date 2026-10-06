package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.OfertaService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.usecase.GestionSolicitudesUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.domain.observer.SolicitudEventPublisher;
import com.findu.core.dto.request.CrearSolicitudRequest;
import com.findu.core.dto.request.ModificarSolicitudRequest;
import com.findu.core.dto.response.SolicitudResponse;
import com.findu.core.exception.ResourceNotFoundException;
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
    private final DireccionService direccionService;
    private final OfertaService ofertaService;
    private final SolicitudEventPublisher eventPublisher;

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

    private SolicitudResponse toResponse(SolicitudServicio s) {
        return new SolicitudResponse(
                s.getId(), null, null, s.getFechaProgramada(), s.getNombreContacto(),
                s.getTelefonoContacto(), s.getPrioridad(), s.getPresupuestoMaximo(), s.getEsPresupuestoEstricto(),
                s.getDetalles(), s.getFotos(), s.getEstadoSolicitud()
        );
    }
}


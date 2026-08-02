package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.usecase.GestionSolicitudesUseCase;
import com.findu.core.domain.model.SolicitudServicio;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionSolicitudesUseCaseImpl implements GestionSolicitudesUseCase {

    private static final Set<String> ESTADOS_MODIFICABLES = Set.of("ABIERTA", "EN_NEGOCIACION");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("ABIERTA", "EN_NEGOCIACION", "PROGRAMADA");

    private final SolicitudServicioService solicitudService;

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
                .cantidadEstimada(request.cantidadEstimada() != null ? request.cantidadEstimada() : 1)
                .estadoSolicitud("ABIERTA")
                .build();

        SolicitudServicio saved = solicitudService.save(solicitud);
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

        if (request.direccionId() != null) solicitud.setDireccionId(request.direccionId());
        if (request.fechaProgramada() != null) solicitud.setFechaProgramada(request.fechaProgramada());
        if (request.nombreContacto() != null) solicitud.setNombreContacto(request.nombreContacto());
        if (request.telefonoContacto() != null) solicitud.setTelefonoContacto(request.telefonoContacto());
        if (request.prioridad() != null) solicitud.setPrioridad(request.prioridad());
        if (request.presupuestoMaximo() != null) solicitud.setPresupuestoMaximo(request.presupuestoMaximo());

        SolicitudServicio updated = solicitudService.save(solicitud);
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
        return toResponse(updated);
    }

    private SolicitudResponse toResponse(SolicitudServicio s) {
        return new SolicitudResponse(
                s.getId(), null, null, s.getFechaProgramada(), s.getNombreContacto(),
                s.getTelefonoContacto(), s.getPrioridad(), s.getPresupuestoMaximo(), s.getEstadoSolicitud()
        );
    }
}

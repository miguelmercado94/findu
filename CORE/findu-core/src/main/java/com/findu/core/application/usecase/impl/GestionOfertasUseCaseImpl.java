package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.OfertaService;
import com.findu.core.application.service.PerfilProveedorService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.usecase.GestionOfertasUseCase;
import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.dto.request.CrearOfertaRequest;
import com.findu.core.dto.response.OfertaResponse;
import com.findu.core.dto.response.SolicitudResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionOfertasUseCaseImpl implements GestionOfertasUseCase {

    private final OfertaService ofertaService;
    private final SolicitudServicioService solicitudService;
    private final PerfilProveedorService proveedorService;

    @Override
    public OfertaResponse enviarOferta(CrearOfertaRequest request) {
        SolicitudServicio solicitud = solicitudService.findById(request.solicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!"ABIERTA".equals(solicitud.getEstadoSolicitud()) &&
                !"EN_NEGOCIACION".equals(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("La solicitud no acepta ofertas en estado: " + solicitud.getEstadoSolicitud());
        }

        if (ofertaService.existsBySolicitudIdAndProveedorId(request.solicitudServicioId(), request.perfilProveedorId())) {
            throw new IllegalStateException("Ya enviaste una oferta para esta solicitud.");
        }

        Oferta oferta = Oferta.builder()
                .solicitudServicioId(request.solicitudServicioId())
                .perfilProveedorId(request.perfilProveedorId())
                .valorPropuesto(request.valorPropuesto())
                .tiempoEstimado(request.tiempoEstimado())
                .mensajePresentacion(request.mensajePresentacion())
                .estadoOferta("ENVIADA")
                .build();

        // Transition solicitud to EN_NEGOCIACION if it was ABIERTA
        if ("ABIERTA".equals(solicitud.getEstadoSolicitud())) {
            solicitud.setEstadoSolicitud("EN_NEGOCIACION");
            solicitudService.save(solicitud);
        }

        Oferta saved = ofertaService.save(oferta);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaResponse> listarOfertasPorSolicitud(Long solicitudId) {
        return ofertaService.findBySolicitudId(solicitudId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OfertaResponse aceptarOferta(Long ofertaId) {
        Oferta oferta = ofertaService.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada con id: " + ofertaId));

        if (!"ENVIADA".equals(oferta.getEstadoOferta())) {
            throw new IllegalStateException("Solo se pueden aceptar ofertas en estado ENVIADA.");
        }

        // Accept this offer
        oferta.setEstadoOferta("ACEPTADA");
        ofertaService.save(oferta);

        // Reject all other offers for the same solicitud
        List<Oferta> otrasOfertas = ofertaService.findBySolicitudId(oferta.getSolicitudServicioId()).stream()
                .filter(o -> !o.getId().equals(ofertaId))
                .peek(o -> o.setEstadoOferta("RECHAZADA"))
                .toList();
        if (!otrasOfertas.isEmpty()) {
            ofertaService.saveAll(otrasOfertas);
        }

        // Transition solicitud to PROGRAMADA
        SolicitudServicio solicitud = solicitudService.findById(oferta.getSolicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        solicitud.setEstadoSolicitud("PROGRAMADA");
        solicitudService.save(solicitud);

        return toResponse(oferta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> obtenerSolicitudesDisponibles(Long proveedorId) {
        return solicitudService.findByEstado("ABIERTA").stream()
                .map(s -> new SolicitudResponse(
                        s.getId(), null, null, s.getFechaProgramada(), s.getNombreContacto(),
                        s.getTelefonoContacto(), s.getPrioridad(), s.getPresupuestoMaximo(), s.getEstadoSolicitud()
                ))
                .toList();
    }

    private OfertaResponse toResponse(Oferta o) {
        PerfilProveedor proveedor = proveedorService.findById(o.getPerfilProveedorId()).orElse(null);
        String nombreProveedor = proveedor != null ? proveedor.getNombreCompleto() : null;
        var calificacion = proveedor != null ? proveedor.getCalificacionPromedio() : null;

        return new OfertaResponse(
                o.getId(), nombreProveedor, calificacion,
                o.getValorPropuesto(), o.getTiempoEstimado(),
                o.getMensajePresentacion(), o.getEstadoOferta()
        );
    }
}

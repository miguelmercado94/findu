package com.findu.core.application.usecase.impl;

import com.findu.core.application.port.output.persistence.ServicioRepositoryPort;
import com.findu.core.application.service.CalificacionService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.usecase.GestionCalificacionesUseCase;
import com.findu.core.domain.model.Calificacion;
import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.domain.model.Servicio;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.dto.request.CrearCalificacionRequest;
import com.findu.core.dto.response.CalificacionResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionCalificacionesUseCaseImpl implements GestionCalificacionesUseCase {

    private final CalificacionService calificacionService;
    private final SolicitudServicioService solicitudService;
    private final PerfilClienteService perfilClienteService;
    private final ServicioRepositoryPort servicioPort;

    @Override
    public CalificacionResponse calificar(CrearCalificacionRequest request) {
        // Validate solicitud exists and is FINALIZADA
        SolicitudServicio solicitud = solicitudService.findById(request.solicitudServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!"FINALIZADA".equals(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("Solo se puede calificar una solicitud en estado FINALIZADA.");
        }

        // Validate one calificación per evaluator per solicitud
        if (calificacionService.existsBySolicitudIdAndEvaluadorId(request.solicitudServicioId(), request.evaluadorId())) {
            throw new IllegalStateException("Ya calificaste esta solicitud.");
        }

        Calificacion calificacion = Calificacion.builder()
                .solicitudServicioId(request.solicitudServicioId())
                .evaluadorId(request.evaluadorId())
                .evaluadoId(request.evaluadoId())
                .tipoEvaluacion(request.tipoEvaluacion())
                .puntaje(request.puntaje())
                .comentario(request.comentario())
                .build();

        Calificacion saved = calificacionService.save(calificacion);
        return new CalificacionResponse(saved.getId(), null, null, saved.getTipoEvaluacion(),
                saved.getPuntaje(), saved.getComentario(), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionResponse> consultarReputacion(Long perfilId) {
        List<Calificacion> calificaciones = calificacionService.findByEvaluadoId(perfilId);

        // Mapa servicioId -> nombre para resolver el servicio de cada solicitud calificada.
        Map<Long, String> nombresServicio = servicioPort.findAll().stream()
                .collect(Collectors.toMap(Servicio::getId, Servicio::getNombre, (a, b) -> a));

        return calificaciones.stream()
                .map(c -> {
                    // Resolver username del evaluador (quien calificó). El evaluador de un proveedor
                    // es un cliente, por lo que buscamos su perfil de cliente.
                    String username = perfilClienteService.findById(c.getEvaluadorId())
                            .map(PerfilCliente::getUsername)
                            .orElse(null);
                    String nombre = perfilClienteService.findById(c.getEvaluadorId())
                            .map(PerfilCliente::getNombreCompleto)
                            .orElse(null);

                    // Resolver el servicio de la solicitud calificada.
                    Long servicioId = solicitudService.findById(c.getSolicitudServicioId())
                            .map(SolicitudServicio::getServicioId)
                            .orElse(null);
                    String servicioNombre = servicioId != null ? nombresServicio.get(servicioId) : null;

                    return new CalificacionResponse(
                            c.getId(),
                            nombre,
                            username,
                            c.getTipoEvaluacion(),
                            c.getPuntaje(),
                            c.getComentario(),
                            servicioId,
                            servicioNombre
                    );
                })
                .toList();
    }
}

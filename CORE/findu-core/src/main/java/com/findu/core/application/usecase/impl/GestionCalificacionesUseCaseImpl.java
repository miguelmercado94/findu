package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.CalificacionService;
import com.findu.core.application.service.SolicitudServicioService;
import com.findu.core.application.usecase.GestionCalificacionesUseCase;
import com.findu.core.domain.model.Calificacion;
import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.dto.request.CrearCalificacionRequest;
import com.findu.core.dto.response.CalificacionResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionCalificacionesUseCaseImpl implements GestionCalificacionesUseCase {

    private final CalificacionService calificacionService;
    private final SolicitudServicioService solicitudService;

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
        return new CalificacionResponse(saved.getId(), null, saved.getTipoEvaluacion(), saved.getPuntaje(), saved.getComentario());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionResponse> consultarReputacion(Long perfilId) {
        return calificacionService.findByEvaluadoId(perfilId).stream()
                .map(c -> new CalificacionResponse(c.getId(), null, c.getTipoEvaluacion(), c.getPuntaje(), c.getComentario()))
                .toList();
    }
}

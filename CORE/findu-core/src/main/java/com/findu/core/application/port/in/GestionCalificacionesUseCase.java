package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearCalificacionRequest;
import com.findu.core.application.dto.response.CalificacionResponse;

import java.util.List;

public interface GestionCalificacionesUseCase {

    CalificacionResponse calificar(CrearCalificacionRequest request);

    List<CalificacionResponse> consultarReputacion(Long perfilId);
}

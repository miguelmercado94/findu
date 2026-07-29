package com.findu.core.application.usecase;

import com.findu.core.dto.request.CrearCalificacionRequest;
import com.findu.core.dto.response.CalificacionResponse;

import java.util.List;

public interface GestionCalificacionesUseCase {

    CalificacionResponse calificar(CrearCalificacionRequest request);

    List<CalificacionResponse> consultarReputacion(Long perfilId);
}

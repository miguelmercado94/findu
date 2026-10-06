package com.findu.core.application.usecase;

import com.findu.core.dto.request.CrearCredencialRequest;
import com.findu.core.dto.response.EspecialistaCredencialResponse;

import java.util.List;

public interface GestionCredencialesUseCase {
    EspecialistaCredencialResponse agregarCredencial(CrearCredencialRequest request);
    List<EspecialistaCredencialResponse> listarPorEspecialidad(Long perfilEspecialistaId);
    void eliminarCredencial(Long id);
}

package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearCredencialRequest;
import com.findu.core.application.dto.response.EspecialistaCredencialResponse;

import java.util.List;

public interface GestionCredencialesUseCase {
    EspecialistaCredencialResponse agregarCredencial(CrearCredencialRequest request);
    List<EspecialistaCredencialResponse> listarPorEspecialidad(Long perfilEspecialistaId);
    void eliminarCredencial(Long id);
}

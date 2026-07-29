package com.findu.core.application.usecase;

import com.findu.core.dto.request.CrearEspecialidadRequest;
import com.findu.core.dto.request.CrearPortafolioRequest;
import com.findu.core.dto.response.PerfilEspecialistaResponse;
import com.findu.core.dto.response.PortafolioItemResponse;

public interface GestionEspecialidadesUseCase {

    PerfilEspecialistaResponse agregarEspecialidad(CrearEspecialidadRequest request);

    void cambiarEstadoEspecialidad(Long id, boolean active);

    void eliminarEspecialidad(Long id);

    PortafolioItemResponse agregarPortafolio(Long especialidadId, CrearPortafolioRequest request);

    void eliminarPortafolio(Long itemId);
}

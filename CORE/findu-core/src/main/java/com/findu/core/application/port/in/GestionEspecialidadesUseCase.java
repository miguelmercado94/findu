package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.CrearEspecialidadRequest;
import com.findu.core.application.dto.request.CrearPortafolioRequest;
import com.findu.core.application.dto.response.PerfilEspecialistaResponse;
import com.findu.core.application.dto.response.PortafolioItemResponse;

public interface GestionEspecialidadesUseCase {

    PerfilEspecialistaResponse agregarEspecialidad(CrearEspecialidadRequest request);

    void cambiarEstadoEspecialidad(Long id, boolean active);

    void eliminarEspecialidad(Long id);

    PortafolioItemResponse agregarPortafolio(Long especialidadId, CrearPortafolioRequest request);

    void eliminarPortafolio(Long itemId);
}

package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.ActualizarPerfilClienteRequest;
import com.findu.core.application.dto.request.CrearPerfilClienteRequest;
import com.findu.core.application.dto.response.PerfilClienteDetalleResponse;
import com.findu.core.application.dto.response.PerfilClienteResponse;

public interface GestionPerfilClienteUseCase {

    PerfilClienteResponse crearPerfil(CrearPerfilClienteRequest request);

    PerfilClienteDetalleResponse consultarPerfil(Long id);

    PerfilClienteDetalleResponse consultarPerfilPorAuthUserId(Long authUserId);

    PerfilClienteResponse actualizarPerfil(Long id, ActualizarPerfilClienteRequest request);

    void cambiarEstado(Long id, String nuevoEstado);
}

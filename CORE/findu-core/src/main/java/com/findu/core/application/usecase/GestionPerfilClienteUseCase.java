package com.findu.core.application.usecase;

import com.findu.core.dto.request.ActualizarPerfilClienteRequest;
import com.findu.core.dto.request.CrearPerfilClienteRequest;
import com.findu.core.dto.response.PerfilClienteDetalleResponse;
import com.findu.core.dto.response.PerfilClienteResponse;

public interface GestionPerfilClienteUseCase {

    PerfilClienteResponse crearPerfil(CrearPerfilClienteRequest request);

    PerfilClienteDetalleResponse consultarPerfil(Long id);

    PerfilClienteResponse actualizarPerfil(Long id, ActualizarPerfilClienteRequest request);

    void cambiarEstado(Long id, String nuevoEstado);
}

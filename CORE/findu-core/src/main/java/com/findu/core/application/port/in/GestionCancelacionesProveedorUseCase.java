package com.findu.core.application.port.in;

import com.findu.core.application.dto.request.AdjuntarEvidenciaCancelacionRequest;
import com.findu.core.application.dto.request.ResolverCancelacionSoporteRequest;
import com.findu.core.application.dto.request.SolicitarCancelacionProveedorRequest;
import com.findu.core.application.dto.response.*;
import com.findu.core.domain.enums.CategoriaCancelacionProveedor;

import java.util.List;

public interface GestionCancelacionesProveedorUseCase {

    List<CategoriaCancelacionCatalogoResponse> obtenerCatalogoCategorias();

    PrevisualizacionCancelacionResponse previsualizarCancelacion(Long proveedorId, CategoriaCancelacionProveedor categoria);

    CancelacionProveedorResponse solicitarCancelacion(SolicitarCancelacionProveedorRequest request);

    CancelacionProveedorResponse adjuntarEvidencias(Long cancelacionId, AdjuntarEvidenciaCancelacionRequest request);

    CancelacionProveedorResponse resolverPorSoporte(Long cancelacionId, ResolverCancelacionSoporteRequest request);

    CancelacionProveedorResponse obtenerCancelacionPorId(Long cancelacionId);

    CancelacionProveedorResponse obtenerCancelacionPorTicket(String ticketCodigo);

    HistorialDisciplinarioProveedorResponse obtenerHistorialDisciplinario(Long proveedorId);

    boolean tieneRestriccionVigente(Long proveedorId);

    void validarProveedorSinRestriccionVigente(Long proveedorId);

    void procesarTicketsFuerzaMayorVencidos();

    void finalizarRestriccionesVencidas();
}


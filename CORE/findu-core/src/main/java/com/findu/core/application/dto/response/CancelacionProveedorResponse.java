package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detalle de una solicitud de cancelación de proveedor y su ticket de soporte")
public record CancelacionProveedorResponse(
        Long id,
        Long solicitudServicioId,
        Long ofertaId,
        Long perfilProveedorId,
        CategoriaCancelacionProveedor categoria,
        String motivo,
        String descripcion,
        List<String> evidenciasUrls,
        String ticketCodigo,
        String estadoCancelacion,
        String estadoTicket,
        String estadoSolicitudActual,
        Boolean esInjustificada,
        LocalDateTime fechaLimiteEvidencia,
        String decisionSoporte,
        String justificacionSoporte,
        Long agenteSoporteId,
        LocalDateTime fechaResolucion,
        Boolean amonestacionGenerada,
        AmonestacionProveedorResponse amonestacionAsociada,
        String mensajeUsuario,
        LocalDateTime createdAt
) {}


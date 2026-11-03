package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.NivelAmonestacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detalle de una amonestación y restricción asociada")
public record AmonestacionProveedorResponse(
        Long id,
        Long perfilProveedorId,
        Long solicitudServicioId,
        Long cancelacionId,
        String ticketCodigo,
        NivelAmonestacion nivel,
        String motivo,
        Integer conteo30Dias,
        Integer diasRestriccion,
        LocalDateTime restriccionInicio,
        LocalDateTime restriccionFin,
        Boolean restriccionActiva,
        Long diasRestriccionRestantes,
        LocalDateTime createdAt
) {}


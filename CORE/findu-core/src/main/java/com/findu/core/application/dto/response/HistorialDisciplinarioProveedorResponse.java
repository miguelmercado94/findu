package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.NivelAmonestacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resumen disciplinario del proveedor (últimos 30 días, restricción vigente e historial)")
public record HistorialDisciplinarioProveedorResponse(
        Long perfilProveedorId,
        int cancelacionesInjustificadasUltimos30Dias,
        int amonestacionesUltimos30Dias,
        NivelAmonestacion nivelActual30Dias,
        boolean tieneRestriccionVigente,
        LocalDateTime restriccionVigenteInicio,
        LocalDateTime restriccionVigenteFin,
        long diasRestriccionRestantes,
        long horasRestriccionRestantes,
        List<AmonestacionProveedorResponse> amonestaciones,
        List<CancelacionProveedorResponse> cancelaciones
) {}


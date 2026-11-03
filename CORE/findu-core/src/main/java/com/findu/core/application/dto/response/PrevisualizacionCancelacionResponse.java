package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import com.findu.core.domain.enums.NivelAmonestacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Advertencia dinámica y proyección de amonestación antes de confirmar una cancelación")
public record PrevisualizacionCancelacionResponse(
        Long perfilProveedorId,
        CategoriaCancelacionProveedor categoria,
        String advertenciaTexto,
        int cancelacionesInjustificadasUltimos30Dias,
        int amonestacionesUltimos30Dias,
        int conteoProyectadoSiConfirma,
        NivelAmonestacion nivelProyectadoSiConfirma,
        int diasRestriccionProyectados,
        String consecuenciaProyectada,
        boolean cancelacionInmediata
) {}


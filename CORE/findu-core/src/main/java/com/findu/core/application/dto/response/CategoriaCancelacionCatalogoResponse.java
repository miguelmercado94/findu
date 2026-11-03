package com.findu.core.application.dto.response;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Categoría y motivos de cancelación disponibles para el proveedor")
public record CategoriaCancelacionCatalogoResponse(
        CategoriaCancelacionProveedor codigo,
        String titulo,
        String advertenciaBase,
        boolean descripcionObligatoria,
        boolean requiereValidacionPreviaSoporte,
        boolean permiteEvidenciaDiferida3Dias,
        List<String> motivos
) {}


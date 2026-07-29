package com.findu.core.dto.request;

import java.math.BigDecimal;

public record ActualizarDireccionRequest(
        String etiqueta,
        String direccionTexto,
        Long municipioId,
        BigDecimal latitud,
        BigDecimal longitud,
        String piso,
        String apartamento,
        String referencia
) {}

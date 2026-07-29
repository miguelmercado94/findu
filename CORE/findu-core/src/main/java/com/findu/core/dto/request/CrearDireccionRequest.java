package com.findu.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearDireccionRequest(
        Long perfilClienteId,
        Long perfilProveedorId,
        String etiqueta,
        @NotBlank String direccionTexto,
        @NotNull Long municipioId,
        BigDecimal latitud,
        BigDecimal longitud,
        String piso,
        String apartamento,
        String referencia,
        boolean esPrincipal
) {}

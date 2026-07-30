package com.findu.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Datos para crear una dirección de cliente")
public record CrearDireccionClienteRequest(
        @Schema(description = "Etiqueta identificadora", example = "Casa")
        String etiqueta,

        @NotBlank @Schema(description = "Dirección en texto", example = "Cra 45 #67-89, Barrio El Poblado")
        String direccionTexto,

        @NotNull @Schema(description = "ID del municipio", example = "2")
        Long municipioId,

        @Schema(description = "Latitud", example = "6.2086")
        BigDecimal latitud,

        @Schema(description = "Longitud", example = "-75.5659")
        BigDecimal longitud,

        @Schema(description = "Piso", example = "3")
        String piso,

        @Schema(description = "Apartamento", example = "301")
        String apartamento,

        @Schema(description = "Referencia", example = "Edificio Torres del Parque")
        String referencia,

        @Schema(description = "Es la dirección principal", example = "true")
        boolean esPrincipal
) {}

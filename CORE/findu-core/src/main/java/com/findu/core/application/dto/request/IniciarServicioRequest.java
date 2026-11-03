package com.findu.core.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Datos para iniciar el servicio (pasar de ACEPTADA a EN_CURSO) con validación de PIN de 4 dígitos y proximidad GPS (máx. 20m)")
public record IniciarServicioRequest(
        @Schema(description = "PIN de 4 dígitos proporcionado por el cliente (en perfil dev usar '1234')", example = "1234")
        String pin,

        @Schema(description = "Latitud GPS actual al iniciar el servicio", example = "6.2442030")
        BigDecimal latitud,

        @Schema(description = "Longitud GPS actual al iniciar el servicio", example = "-75.5812119")
        BigDecimal longitud,

        @Schema(description = "ID del proveedor o actor que ejecuta el cambio (opcional)", example = "10")
        Long actorId
) {}


package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo del POST /api/v1/activation/biometric. El face match se resuelve en cliente (TFLite);
 * el backend re-valida nombre (sin tildes) y fecha de nacimiento contra findu-core.
 */
public record BiometricSubmitRequest(
        @Schema(example = "Carlos Alberto Mendoza")
        @NotBlank(message = "cedulaNombre es obligatorio")
        @Size(max = 200, message = "cedulaNombre excede 200 caracteres")
        String cedulaNombre,

        @Schema(example = "1994-11-01")
        @NotBlank(message = "cedulaFechaNacimiento es obligatorio")
        String cedulaFechaNacimiento,

        @Schema(example = "0.97")
        @DecimalMin(value = "0.0", message = "faceMatchScore fuera de rango")
        @DecimalMax(value = "1.0", message = "faceMatchScore fuera de rango")
        BigDecimal faceMatchScore,

        @Schema(example = "true")
        @NotNull(message = "faceMatched es obligatorio")
        Boolean faceMatched,

        @Schema(example = "https://cdn.findu.example/frontal.jpg")
        @NotBlank(message = "fotoFrontalUrl es obligatorio")
        String fotoFrontalUrl
) {}

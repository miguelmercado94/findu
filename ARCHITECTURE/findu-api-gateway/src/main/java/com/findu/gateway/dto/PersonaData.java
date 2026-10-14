package com.findu.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record PersonaData(
        @NotBlank String nombreCompleto,
        @NotBlank String tipoDocumento,
        @NotBlank String numeroDocumento,
        String fechaNacimiento,
        String sexo
) {
}

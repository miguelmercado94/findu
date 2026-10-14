package com.findu.gateway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnboardingRequest(
        @NotBlank String flowType,
        Boolean isFederated,
        FederatedData federated,
        @NotNull @Valid SecurityData security,
        @NotNull @Valid PersonaData persona,
        @NotNull @Valid DireccionData direccion,
        CoberturaData cobertura,
        List<EspecialidadItem> especialidades,
        BiometriaData biometria,
        String idempotencyKey
) {
}

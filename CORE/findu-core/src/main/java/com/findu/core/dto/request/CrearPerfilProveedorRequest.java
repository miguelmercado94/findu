package com.findu.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CrearPerfilProveedorRequest(
        @NotNull Long authUserId,
        @NotBlank String nombreCompleto,
        @NotBlank String numeroIdentificacion,
        @NotBlank String tipoIdentificacion,
        LocalDate fechaNacimiento,
        String sexo,
        @NotBlank String celular,
        String codPhoneInternational
) {}

package com.findu.security.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileDto(
        @NotBlank(message = "username es requerido")
        String username,

        @NotBlank(message = "phone es requerido")
        String phone,

        @NotBlank(message = "codPhoneInternational es requerido")
        String codPhoneInternational
) {}

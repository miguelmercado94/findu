package com.findu.validacion.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SendPhoneOtpRequest(
        @NotBlank(message = "phone es obligatorio")
        String phone,

        String codPhoneInternational,

        String processCode
) {}

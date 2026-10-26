package com.findu.validacion.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPhoneOtpRequest(
        @NotBlank(message = "phone es obligatorio")
        String phone,

        String codPhoneInternational,

        @NotBlank(message = "code es obligatorio")
        String code,

        String processCode
) {}

package com.findu.validacion.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterBrebKeyRequest(
        @NotNull(message = "providerId es obligatorio")
        Long providerId,

        @NotBlank(message = "keyType es obligatorio (CELULAR, CEDULA, CORREO, ALFANUMERICO)")
        String keyType,

        @NotBlank(message = "keyValue es obligatorio")
        String keyValue,

        @NotBlank(message = "bank es obligatorio")
        String bank,

        @NotBlank(message = "phone es obligatorio para validación OTP")
        String phone,

        String codPhoneInternational,

        @NotBlank(message = "otpCode es obligatorio para autorizar la vinculación")
        String otpCode
) {}

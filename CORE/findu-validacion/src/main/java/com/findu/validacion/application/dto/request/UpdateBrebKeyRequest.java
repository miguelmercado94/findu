package com.findu.validacion.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateBrebKeyRequest(
        @NotBlank(message = "keyType es obligatorio (CELULAR, CEDULA, CORREO, ALFANUMERICO)")
        String keyType,

        @NotBlank(message = "keyValue es obligatorio")
        String keyValue,

        @NotBlank(message = "bank es obligatorio")
        String bank,

        @NotBlank(message = "phone es obligatorio para validación OTP")
        String phone,

        String codPhoneInternational,

        @NotBlank(message = "otpCode es obligatorio para autorizar el cambio de llave")
        String otpCode
) {}

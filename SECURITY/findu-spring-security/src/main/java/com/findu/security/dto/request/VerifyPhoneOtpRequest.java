package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de verificación del OTP de teléfono del proveedor.
 */
public record VerifyPhoneOtpRequest(
        @Schema(example = "3003763301")
        @NotBlank(message = "El teléfono es obligatorio")
        String phone,
        @Schema(example = "123456")
        @NotBlank(message = "El código es obligatorio")
        String code,
        @Schema(example = "+57", description = "Código de país. Por defecto +57 (Colombia)")
        String codPhoneInternational
) {}

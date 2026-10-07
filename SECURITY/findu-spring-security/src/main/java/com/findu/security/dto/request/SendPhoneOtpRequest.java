package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de envío de OTP de verificación de teléfono del proveedor.
 */
public record SendPhoneOtpRequest(
        @Schema(example = "3003763301")
        @NotBlank(message = "El teléfono es obligatorio")
        String phone,
        @Schema(example = "+57", description = "Código de país. Por defecto +57 (Colombia)")
        String codPhoneInternational
) {}

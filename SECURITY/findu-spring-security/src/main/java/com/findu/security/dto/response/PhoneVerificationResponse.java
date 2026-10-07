package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de la verificación del OTP de teléfono del proveedor.
 */
public record PhoneVerificationResponse(
        @Schema(example = "true")
        boolean phoneVerified,
        @Schema(example = "PENDING_EMAIL_VERIFICATION")
        String accountStatus
) {}

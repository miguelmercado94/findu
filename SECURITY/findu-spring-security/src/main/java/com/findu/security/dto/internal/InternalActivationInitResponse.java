package com.findu.security.dto.internal;

public record InternalActivationInitResponse(
        String estadoActivacion,
        boolean otpOk,
        boolean emailOk,
        boolean biometriaOk
) {
}

package com.findu.validacion.application.dto.response;

public record VerifyPhoneOtpResponse(
        boolean verified,
        String message
) {}

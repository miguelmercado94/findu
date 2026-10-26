package com.findu.validacion.application.dto.response;

public record SendPhoneOtpResponse(
        String status,
        int expiresInSeconds,
        String message
) {}

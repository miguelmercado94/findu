package com.findu.gateway.dto;

public record SagaErrorResponse(
        String error,
        String step,
        boolean compensated,
        String message
) {
}

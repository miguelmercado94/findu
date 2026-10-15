package com.findu.validacion.application.dto.response;

public record FailedValidationDto(
        String validationCode,
        String errorMessageKey
) {
}

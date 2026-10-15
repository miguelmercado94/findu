package com.findu.validacion.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.findu.validacion.domain.enums.ExecutionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PassiveValidationDto(
        String validationCode,
        String alertTemplateCode,
        ExecutionType executionType,
        String resolutionEndpoint
) {
}

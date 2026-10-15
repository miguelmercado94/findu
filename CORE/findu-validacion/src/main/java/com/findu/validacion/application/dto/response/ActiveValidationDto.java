package com.findu.validacion.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActiveValidationDto(
        String validationCode,
        @JsonProperty("isMandatory")
        boolean isMandatory,
        String resolutionEndpoint
) {
}

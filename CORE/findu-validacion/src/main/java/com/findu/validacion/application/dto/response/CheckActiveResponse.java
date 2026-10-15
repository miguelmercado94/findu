package com.findu.validacion.application.dto.response;

import java.util.List;

public record CheckActiveResponse(
        boolean allowed,
        List<FailedValidationDto> failed
) {
}

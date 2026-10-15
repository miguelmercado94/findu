package com.findu.validacion.application.dto.response;

import java.util.List;

public record InspectResponse(
        String processCode,
        List<ActiveValidationDto> active,
        List<PassiveValidationDto> passive
) {
}

package com.findu.validacion.application.dto.response;

import java.util.List;

public record CompleteProcessResponse(
        String processCode,
        List<String> pending,
        int alertsEmitted
) {
}

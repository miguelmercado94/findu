package com.findu.validacion.application.dto.request;

import java.util.Map;

public record ValidationEvidenceRequest(
        String validationCode,
        Map<String, Object> payload
) {
}

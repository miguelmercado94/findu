package com.findu.validacion.domain.model;

import com.findu.validacion.domain.enums.ExecutionType;
import java.util.Map;

public record ValidationDefinition(
        String id,
        String validationCode,
        String name,
        String description,
        ExecutionType executionType,
        String resolutionEndpoint,
        Map<String, Object> payloadSchema,
        String alertTemplateCode
) {
}

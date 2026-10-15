package com.findu.validacion.domain.model;

import com.findu.validacion.domain.enums.ValidationMode;
import java.util.List;

public record EndpointValidationConfig(
        String id,
        String processCode,
        List<ConfigItem> validations
) {
    public record ConfigItem(
            String validationCode,
            ValidationMode mode,
            int executionOrder,
            boolean mandatory,
            String errorMessageKey
    ) {
    }
}

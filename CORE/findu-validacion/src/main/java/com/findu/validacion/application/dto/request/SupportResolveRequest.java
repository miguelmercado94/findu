package com.findu.validacion.application.dto.request;

public record SupportResolveRequest(
        String processCode,
        String entityId,
        String entityType,
        String validationCode,
        String resolution,
        Long reviewerId,
        String nota
) {
}

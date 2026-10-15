package com.findu.validacion.application.dto.request;

import java.util.List;

public record CompleteProcessRequest(
        String processCode,
        String entityId,
        Long userId,
        String entityType,
        List<String> satisfied,
        List<ValidationEvidenceRequest> evidences
) {
}

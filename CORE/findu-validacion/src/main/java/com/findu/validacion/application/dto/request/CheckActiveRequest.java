package com.findu.validacion.application.dto.request;

import java.util.List;

public record CheckActiveRequest(
        String processCode,
        String entityId,
        List<ValidationEvidenceRequest> evidences
) {
}

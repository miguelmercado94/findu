package com.findu.validacion.domain.model;

import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record ProcessValidationInstance(
        String id,
        String entityType,
        String entityId,
        String processCode,
        String validationCode,
        ValidationMode mode,
        ValidationStatus status,
        Map<String, Object> evidence,
        Long reviewerId,
        String alertUuid,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
    public ProcessValidationInstance resolve(ValidationStatus newStatus, Long reviewer, String note) {
        Map<String, Object> newEvidence = evidence != null ? new HashMap<>(evidence) : new HashMap<>();
        if (note != null && !note.isBlank()) {
            newEvidence.put("resolutionNote", note);
        }
        return new ProcessValidationInstance(
                this.id,
                this.entityType,
                this.entityId,
                this.processCode,
                this.validationCode,
                this.mode,
                newStatus,
                newEvidence,
                reviewer,
                this.alertUuid,
                this.createdAt,
                Instant.now(),
                this.createdBy,
                reviewer != null ? "REVIEWER_" + reviewer : "SYSTEM"
        );
    }
}

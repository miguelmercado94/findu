package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ProcessValidationInstanceDocument;
import org.springframework.stereotype.Component;

@Component
public class ProcessValidationInstanceMapper {

    public ProcessValidationInstance toDomain(ProcessValidationInstanceDocument doc) {
        if (doc == null) {
            return null;
        }
        ValidationMode mode = doc.getMode() != null
                ? ValidationMode.valueOf(doc.getMode().toUpperCase())
                : null;
        ValidationStatus status = doc.getStatus() != null
                ? ValidationStatus.valueOf(doc.getStatus().toUpperCase())
                : null;

        return new ProcessValidationInstance(
                doc.getId(),
                doc.getEntityType(),
                doc.getEntityId(),
                doc.getProcessCode(),
                doc.getValidationCode(),
                mode,
                status,
                doc.getEvidence(),
                doc.getReviewerId(),
                doc.getAlertUuid(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                doc.getCreatedBy(),
                doc.getUpdatedBy()
        );
    }

    public ProcessValidationInstanceDocument toDocument(ProcessValidationInstance domain) {
        if (domain == null) {
            return null;
        }
        return ProcessValidationInstanceDocument.builder()
                .id(domain.id())
                .entityType(domain.entityType())
                .entityId(domain.entityId())
                .processCode(domain.processCode())
                .validationCode(domain.validationCode())
                .mode(domain.mode() != null ? domain.mode().name() : null)
                .status(domain.status() != null ? domain.status().name() : null)
                .evidence(domain.evidence())
                .reviewerId(domain.reviewerId())
                .alertUuid(domain.alertUuid())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .createdBy(domain.createdBy())
                .updatedBy(domain.updatedBy())
                .build();
    }
}

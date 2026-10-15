package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.enums.ExecutionType;
import com.findu.validacion.domain.model.ValidationDefinition;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ValidationDefinitionDocument;
import org.springframework.stereotype.Component;

@Component
public class ValidationDefinitionMapper {

    public ValidationDefinition toDomain(ValidationDefinitionDocument doc) {
        if (doc == null) {
            return null;
        }
        ExecutionType type = doc.getExecutionType() != null
                ? ExecutionType.valueOf(doc.getExecutionType().toUpperCase())
                : null;
        return new ValidationDefinition(
                doc.getId(),
                doc.getValidationCode(),
                doc.getName(),
                doc.getDescription(),
                type,
                doc.getResolutionEndpoint(),
                doc.getPayloadSchema(),
                doc.getAlertTemplateCode()
        );
    }

    public ValidationDefinitionDocument toDocument(ValidationDefinition domain) {
        if (domain == null) {
            return null;
        }
        return ValidationDefinitionDocument.builder()
                .id(domain.id())
                .validationCode(domain.validationCode())
                .name(domain.name())
                .description(domain.description())
                .executionType(domain.executionType() != null ? domain.executionType().name() : null)
                .resolutionEndpoint(domain.resolutionEndpoint())
                .payloadSchema(domain.payloadSchema())
                .alertTemplateCode(domain.alertTemplateCode())
                .build();
    }
}

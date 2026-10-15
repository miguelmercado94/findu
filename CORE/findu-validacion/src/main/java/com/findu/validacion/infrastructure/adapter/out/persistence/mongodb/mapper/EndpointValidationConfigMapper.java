package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.enums.ValidationMode;
import com.findu.validacion.domain.model.EndpointValidationConfig;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.EndpointValidationConfigDocument;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class EndpointValidationConfigMapper {

    public EndpointValidationConfig toDomain(EndpointValidationConfigDocument doc) {
        if (doc == null) {
            return null;
        }
        List<EndpointValidationConfig.ConfigItem> items = doc.getValidations() != null
                ? doc.getValidations().stream()
                .map(this::toConfigItemDomain)
                .toList()
                : Collections.emptyList();

        return new EndpointValidationConfig(doc.getId(), doc.getProcessCode(), items);
    }

    public EndpointValidationConfigDocument toDocument(EndpointValidationConfig domain) {
        if (domain == null) {
            return null;
        }
        List<EndpointValidationConfigDocument.ConfigItemDocument> items = domain.validations() != null
                ? domain.validations().stream()
                .map(this::toConfigItemDocument)
                .toList()
                : Collections.emptyList();

        return EndpointValidationConfigDocument.builder()
                .id(domain.id())
                .processCode(domain.processCode())
                .validations(items)
                .build();
    }

    private EndpointValidationConfig.ConfigItem toConfigItemDomain(EndpointValidationConfigDocument.ConfigItemDocument doc) {
        if (doc == null) {
            return null;
        }
        ValidationMode mode = doc.getMode() != null
                ? ValidationMode.valueOf(doc.getMode().toUpperCase())
                : null;
        return new EndpointValidationConfig.ConfigItem(
                doc.getValidationCode(),
                mode,
                doc.getExecutionOrder(),
                doc.isMandatory(),
                doc.getErrorMessageKey()
        );
    }

    private EndpointValidationConfigDocument.ConfigItemDocument toConfigItemDocument(EndpointValidationConfig.ConfigItem domain) {
        if (domain == null) {
            return null;
        }
        return EndpointValidationConfigDocument.ConfigItemDocument.builder()
                .validationCode(domain.validationCode())
                .mode(domain.mode() != null ? domain.mode().name() : null)
                .executionOrder(domain.executionOrder())
                .mandatory(domain.mandatory())
                .errorMessageKey(domain.errorMessageKey())
                .build();
    }
}

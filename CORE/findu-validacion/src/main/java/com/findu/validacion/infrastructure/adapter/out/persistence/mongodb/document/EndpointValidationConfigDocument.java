package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "endpoint_validation_configs")
public class EndpointValidationConfigDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String processCode;

    private List<ConfigItemDocument> validations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigItemDocument {
        private String validationCode;
        private String mode;
        private int executionOrder;
        private boolean mandatory;
        private String errorMessageKey;
    }
}

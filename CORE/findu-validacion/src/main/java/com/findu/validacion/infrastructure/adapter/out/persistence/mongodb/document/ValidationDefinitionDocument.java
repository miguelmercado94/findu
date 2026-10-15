package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "validation_definitions")
public class ValidationDefinitionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String validationCode;

    private String name;

    private String description;

    private String executionType;

    private String resolutionEndpoint;

    private Map<String, Object> payloadSchema;

    private String alertTemplateCode;
}

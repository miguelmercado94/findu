package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "process_validation_instances")
@CompoundIndexes({
        @CompoundIndex(name = "uq_instance", def = "{'entityType': 1, 'entityId': 1, 'processCode': 1, 'validationCode': 1}", unique = true),
        @CompoundIndex(name = "ix_process_validation_mode_status", def = "{'processCode': 1, 'mode': 1, 'status': 1}"),
        @CompoundIndex(name = "ix_entity_status", def = "{'entityType': 1, 'entityId': 1, 'status': 1}")
})
public class ProcessValidationInstanceDocument {

    @Id
    private String id;

    private String entityType;

    private String entityId;

    private String processCode;

    private String validationCode;

    private String mode;

    private String status;

    private Map<String, Object> evidence;

    private Long reviewerId;

    private String alertUuid;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;
}

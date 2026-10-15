package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "monitored_endpoints")
@CompoundIndex(name = "uq_monitored_endpoint", def = "{'serviceName': 1, 'httpMethod': 1, 'pathPattern': 1}", unique = true)
public class MonitoredEndpointDocument {

    @Id
    private String id;

    private String serviceName;

    private String httpMethod;

    private String pathPattern;

    @Indexed(unique = true)
    private String processCode;

    private String description;

    private boolean active;

    private Instant createdAt;
}

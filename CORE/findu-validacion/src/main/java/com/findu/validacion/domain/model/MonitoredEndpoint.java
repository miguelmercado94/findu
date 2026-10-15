package com.findu.validacion.domain.model;

import com.findu.validacion.domain.enums.HttpMethod;
import java.time.Instant;

public record MonitoredEndpoint(
        String id,
        String serviceName,
        HttpMethod httpMethod,
        String pathPattern,
        String processCode,
        String description,
        boolean active,
        Instant createdAt
) {
}

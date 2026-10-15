package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.model.MonitoredEndpoint;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.MonitoredEndpointDocument;
import org.springframework.stereotype.Component;

@Component
public class MonitoredEndpointMapper {

    public MonitoredEndpoint toDomain(MonitoredEndpointDocument doc) {
        if (doc == null) {
            return null;
        }
        HttpMethod method = doc.getHttpMethod() != null
                ? HttpMethod.valueOf(doc.getHttpMethod().toUpperCase())
                : null;
        return new MonitoredEndpoint(
                doc.getId(),
                doc.getServiceName(),
                method,
                doc.getPathPattern(),
                doc.getProcessCode(),
                doc.getDescription(),
                doc.isActive(),
                doc.getCreatedAt()
        );
    }

    public MonitoredEndpointDocument toDocument(MonitoredEndpoint domain) {
        if (domain == null) {
            return null;
        }
        return MonitoredEndpointDocument.builder()
                .id(domain.id())
                .serviceName(domain.serviceName())
                .httpMethod(domain.httpMethod() != null ? domain.httpMethod().name() : null)
                .pathPattern(domain.pathPattern())
                .processCode(domain.processCode())
                .description(domain.description())
                .active(domain.active())
                .createdAt(domain.createdAt())
                .build();
    }
}

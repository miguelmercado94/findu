package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.model.ProviderBrebKey;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.ProviderBrebKeyDocument;
import org.springframework.stereotype.Component;

@Component
public class ProviderBrebKeyMapper {

    public ProviderBrebKey toDomain(ProviderBrebKeyDocument doc) {
        if (doc == null) {
            return null;
        }
        return new ProviderBrebKey(
                doc.getId(),
                doc.getProviderId(),
                doc.getKeyType(),
                doc.getKeyValue(),
                doc.getBank(),
                doc.getPhone(),
                doc.getCodPhoneInternational(),
                doc.getStatus(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public ProviderBrebKeyDocument toDocument(ProviderBrebKey domain) {
        if (domain == null) {
            return null;
        }
        return ProviderBrebKeyDocument.builder()
                .id(domain.id())
                .providerId(domain.providerId())
                .keyType(domain.keyType())
                .keyValue(domain.keyValue())
                .bank(domain.bank())
                .phone(domain.phone())
                .codPhoneInternational(domain.codPhoneInternational())
                .status(domain.status())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}

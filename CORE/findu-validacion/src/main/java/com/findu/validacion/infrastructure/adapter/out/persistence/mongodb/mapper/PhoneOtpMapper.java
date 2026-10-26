package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper;

import com.findu.validacion.domain.model.PhoneOtpRecord;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.PhoneOtpDocument;
import org.springframework.stereotype.Component;

@Component
public class PhoneOtpMapper {

    public PhoneOtpRecord toDomain(PhoneOtpDocument doc) {
        if (doc == null) {
            return null;
        }
        return new PhoneOtpRecord(
                doc.getId(),
                doc.getPhoneKey(),
                doc.getCode(),
                doc.getProcessCode(),
                doc.getExpiresAt(),
                doc.isVerified(),
                doc.getVerifiedAt(),
                doc.getCreatedAt()
        );
    }

    public PhoneOtpDocument toDocument(PhoneOtpRecord domain) {
        if (domain == null) {
            return null;
        }
        return PhoneOtpDocument.builder()
                .id(domain.id())
                .phoneKey(domain.phoneKey())
                .code(domain.code())
                .processCode(domain.processCode())
                .expiresAt(domain.expiresAt())
                .verified(domain.verified())
                .verifiedAt(domain.verifiedAt())
                .createdAt(domain.createdAt())
                .build();
    }
}

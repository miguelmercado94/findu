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
@Document(collection = "phone_otp_validations")
@CompoundIndex(name = "ix_phone_process", def = "{'phoneKey': 1, 'processCode': 1}")
public class PhoneOtpDocument {

    @Id
    private String id;

    private String phoneKey;

    private String code;

    private String processCode;

    @Indexed(name = "ix_otp_ttl", expireAfter = "0s")
    private Instant expiresAt;

    private boolean verified;

    private Instant verifiedAt;

    private Instant createdAt;
}

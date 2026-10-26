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
@Document(collection = "provider_breb_keys")
@CompoundIndex(name = "uq_provider_status", def = "{'providerId': 1, 'status': 1}")
public class ProviderBrebKeyDocument {

    @Id
    private String id;

    @Indexed
    private Long providerId;

    private String keyType;

    private String keyValue;

    private String bank;

    private String phone;

    private String codPhoneInternational;

    private String status;

    private Instant createdAt;

    private Instant updatedAt;
}

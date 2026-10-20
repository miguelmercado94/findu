package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "provider_settlement_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSettlementProfileEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private Long providerId;

    @Column(name = "provider_type", length = 32, nullable = false)
    private String providerType;

    @Column(name = "settlement_frequency", length = 32, nullable = false)
    private String settlementFrequency;

    @Column(name = "settlement_day", length = 32)
    private String settlementDay;

    @Column(name = "settlement_time", length = 16)
    private String settlementTime;

    @Column(name = "negative_balance_limit", precision = 19, scale = 2, nullable = false)
    private BigDecimal negativeBalanceLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

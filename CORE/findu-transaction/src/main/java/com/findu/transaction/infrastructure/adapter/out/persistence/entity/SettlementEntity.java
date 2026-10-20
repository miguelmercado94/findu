package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "settlement_code", length = 64, nullable = false, unique = true)
    private String settlementCode;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "provider_type", length = 32, nullable = false)
    private String providerType;

    @Column(name = "gross_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "debt_compensation_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal debtCompensationAmount;

    @Column(name = "net_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal netAmount;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "settlement_type", length = 32)
    private String settlementType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

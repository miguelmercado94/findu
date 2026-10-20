package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "payout_code", length = 64, nullable = false, unique = true)
    private String payoutCode;

    @Column(name = "settlement_id", length = 36, unique = true)
    private String settlementId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "destination", length = 255)
    private String destination;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

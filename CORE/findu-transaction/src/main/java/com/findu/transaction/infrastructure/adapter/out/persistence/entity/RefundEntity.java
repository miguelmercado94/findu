package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "refund_code", length = 64, nullable = false, unique = true)
    private String refundCode;

    @Column(name = "transaction_id", length = 36, nullable = false)
    private String transactionId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}

package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformCommissionEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "transaction_id", length = 36, nullable = false)
    private String transactionId;

    @Column(name = "rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal rate;

    @Column(name = "base_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "commission_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

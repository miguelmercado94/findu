package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "provider_debt_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDebtPaymentEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "payment_code", length = 64, nullable = false, unique = true)
    private String paymentCode;

    @Column(name = "provider_debt_id", length = 36)
    private String providerDebtId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 32, nullable = false)
    private String paymentMethod;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

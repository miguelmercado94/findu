package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bre_b_payment_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreBPaymentOrderEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_code", length = 64, nullable = false, unique = true)
    private String orderCode;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "bre_b_key", length = 64, nullable = false)
    private String breBKey;

    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false)
    private String qrPayload;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "external_transaction_id", length = 128)
    private String externalTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

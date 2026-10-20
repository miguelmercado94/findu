package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "transaction_id", length = 36, nullable = false)
    private String transactionId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "method", length = 32, nullable = false)
    private String method;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "reference", length = 128, unique = true)
    private String reference;

    @Column(name = "provider_token", length = 128)
    private String providerToken;

    @Column(name = "brand", length = 32)
    private String brand;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(name = "expiration_month")
    private Integer expirationMonth;

    @Column(name = "expiration_year")
    private Integer expirationYear;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

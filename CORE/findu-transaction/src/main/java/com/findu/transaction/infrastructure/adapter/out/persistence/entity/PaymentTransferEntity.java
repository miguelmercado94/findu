package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransferEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;

    @Column(name = "source_account", length = 128)
    private String sourceAccount;

    @Column(name = "destination_account", length = 128)
    private String destinationAccount;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 32, nullable = false)
    private String paymentMethod;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}

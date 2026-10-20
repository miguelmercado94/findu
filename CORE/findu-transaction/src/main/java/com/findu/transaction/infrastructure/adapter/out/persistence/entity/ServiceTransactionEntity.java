package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "service_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceTransactionEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "transaction_code", length = 64, nullable = false, unique = true)
    private String transactionCode;

    @Column(name = "service_request_id", nullable = false)
    private Long serviceRequestId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "service_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal serviceAmount;

    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "tip_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal tipAmount;

    @Column(name = "extra_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal extraAmount;

    @Column(name = "total_customer_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCustomerAmount;

    @Column(name = "provider_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal providerAmount;

    @Column(name = "payment_method", length = 32, nullable = false)
    private String paymentMethod;

    @Column(name = "payment_status", length = 32, nullable = false)
    private String paymentStatus;

    @Column(name = "transaction_status", length = 32, nullable = false)
    private String transactionStatus;

    @Column(name = "service_completed_at")
    private Instant serviceCompletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

package com.findu.transaction.dailybalance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "service_transactions")
@Getter
@Setter
public class ServiceTransactionEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 50)
    private String transactionCode;

    @Column(name = "service_request_id", nullable = false)
    private Long serviceRequestId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "service_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal serviceAmount;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "tip_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "extra_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal extraAmount;

    @Column(name = "total_customer_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCustomerAmount;

    @Column(name = "provider_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal providerAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "transaction_status", nullable = false, length = 20)
    private String transactionStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

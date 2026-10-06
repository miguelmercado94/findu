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
@Table(name = "provider_accounts")
@Getter
@Setter
public class ProviderAccountEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private Long providerId;

    @Column(name = "provider_type", nullable = false, length = 30)
    private String providerType;

    @Column(name = "payable_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal payableBalance;

    @Column(name = "receivable_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal receivableBalance;

    @Column(name = "debt_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal debtLimit;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

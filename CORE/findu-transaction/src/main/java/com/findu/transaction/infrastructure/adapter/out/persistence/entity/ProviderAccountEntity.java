package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "provider_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderAccountEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private Long providerId;

    @Column(name = "provider_type", length = 32, nullable = false)
    private String providerType;

    @Column(name = "payable_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal payableBalance;

    @Column(name = "receivable_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal receivableBalance;

    @Column(name = "last_balance_date")
    private LocalDate lastBalanceDate;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "COP";

    @Column(name = "debt_limit", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal debtLimit = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

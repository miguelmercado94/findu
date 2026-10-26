package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "daily_balances",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_balance_provider_date", columnNames = {"provider_id", "balance_date"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyBalanceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(name = "opening_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "total_earnings", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalEarnings;

    @Column(name = "total_commissions", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCommissions;

    @Column(name = "total_tips", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalTips;

    @Column(name = "total_extras", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalExtras;

    @Column(name = "cash_payments", precision = 19, scale = 2, nullable = false)
    private BigDecimal cashPayments;

    @Column(name = "digital_payments", precision = 19, scale = 2, nullable = false)
    private BigDecimal digitalPayments;

    @Column(name = "provider_debt", precision = 19, scale = 2, nullable = false)
    private BigDecimal providerDebt;

    @Column(name = "pending_payout", precision = 19, scale = 2, nullable = false)
    private BigDecimal pendingPayout;

    @Column(name = "closing_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal closingBalance;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "COP";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

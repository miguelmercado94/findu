package com.findu.transaction.dailybalance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_balances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_balance_provider_date", columnNames = {"provider_id", "balance_date"})
})
@Getter
@Setter
public class DailyBalanceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "total_earnings", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEarnings;

    @Column(name = "total_commissions", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCommissions;

    @Column(name = "total_tips", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTips;

    @Column(name = "total_extras", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalExtras;

    @Column(name = "cash_payments", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashPayments;

    @Column(name = "digital_payments", nullable = false, precision = 19, scale = 2)
    private BigDecimal digitalPayments;

    @Column(name = "provider_debt", nullable = false, precision = 19, scale = 2)
    private BigDecimal providerDebt;

    @Column(name = "pending_payout", nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingPayout;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

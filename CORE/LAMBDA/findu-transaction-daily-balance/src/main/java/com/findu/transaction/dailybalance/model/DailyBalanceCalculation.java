package com.findu.transaction.dailybalance.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DailyBalanceCalculation {
    private final Long providerId;
    private final LocalDate balanceDate;
    private final BigDecimal openingBalance;
    private final BigDecimal totalEarnings;
    private final BigDecimal totalCommissions;
    private final BigDecimal totalTips;
    private final BigDecimal totalExtras;
    private final BigDecimal cashPayments;
    private final BigDecimal digitalPayments;
    private final BigDecimal providerDebt;
    private final BigDecimal pendingPayout;
    private final BigDecimal closingBalance;
    private final String currency;
}

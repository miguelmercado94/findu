package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class DailyBalanceResponse {
    private String id;
    private Long providerId;
    private LocalDate balanceDate;
    private BigDecimal openingBalance;
    private BigDecimal totalEarnings;
    private BigDecimal totalCommissions;
    private BigDecimal totalTips;
    private BigDecimal totalExtras;
    private BigDecimal cashPayments;
    private BigDecimal digitalPayments;
    private BigDecimal providerDebt;
    private BigDecimal pendingPayout;
    private BigDecimal closingBalance;
    private String currency;
    private Instant createdAt;
}

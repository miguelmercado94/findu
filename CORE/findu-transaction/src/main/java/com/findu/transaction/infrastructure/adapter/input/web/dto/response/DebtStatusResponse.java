package com.findu.transaction.infrastructure.adapter.input.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DebtStatusResponse {
    private Long providerId;
    private BigDecimal currentDebt;
    private BigDecimal debtLimit;
    private double usedPercentage;
    private BigDecimal remainingBeforeBlock;
    private String currency;
    private String restrictionStatus; // NORMAL, WARNING, RESTRICTED
    private boolean isBlocked;
    private String restrictionMessage;
}

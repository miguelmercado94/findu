package com.findu.transaction.application.port.in.finance.dto;

import com.findu.transaction.domain.enums.FinanceRestrictionStatus;
import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DebtStatusResult {
    private final Long providerId;
    private final Money currentDebt;
    private final Money debtLimit;
    private final double usedPercentage;
    private final Money remainingBeforeBlock;
    private final FinanceRestrictionStatus restrictionStatus;
    private final boolean isBlocked;
    private final String restrictionMessage;
}

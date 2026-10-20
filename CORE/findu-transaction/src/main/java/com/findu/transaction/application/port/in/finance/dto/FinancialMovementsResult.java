package com.findu.transaction.application.port.in.finance.dto;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FinancialMovementsResult {
    private final String filter; // THIS_WEEK, LAST_30_DAYS, ALL
    private final String periodLabel;
    private final Money totalIncome;
    private final Money totalExpenses;
    private final Money netBalance;
    private final List<FinancialMovementItem> movements;
}

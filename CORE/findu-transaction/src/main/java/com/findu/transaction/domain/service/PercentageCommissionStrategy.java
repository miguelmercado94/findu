package com.findu.transaction.domain.service;

import com.findu.transaction.domain.valueobject.Money;

import java.math.BigDecimal;

public class PercentageCommissionStrategy implements CommissionCalculationStrategy {

    @Override
    public Money calculateCommission(Money serviceAmount, BigDecimal rate) {
        if (serviceAmount == null) {
            return Money.ZERO;
        }
        BigDecimal appliedRate = rate != null ? rate : new BigDecimal("0.10"); // Default 10%
        return serviceAmount.multiply(appliedRate);
    }
}

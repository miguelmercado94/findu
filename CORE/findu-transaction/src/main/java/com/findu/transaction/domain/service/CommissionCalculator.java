package com.findu.transaction.domain.service;

import com.findu.transaction.domain.valueobject.Money;

import java.math.BigDecimal;

public class CommissionCalculator {

    private final CommissionCalculationStrategy strategy;

    public CommissionCalculator(CommissionCalculationStrategy strategy) {
        this.strategy = strategy != null ? strategy : new PercentageCommissionStrategy();
    }

    public CommissionCalculator() {
        this(new PercentageCommissionStrategy());
    }

    public Money calculate(Money serviceAmount, BigDecimal rate) {
        return strategy.calculateCommission(serviceAmount, rate);
    }
}

package com.findu.transaction.domain.service;

import com.findu.transaction.domain.valueobject.Money;

import java.math.BigDecimal;

public interface CommissionCalculationStrategy {
    Money calculateCommission(Money serviceAmount, BigDecimal rate);
}

package com.findu.transaction.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Getter
@EqualsAndHashCode
public class Money {

    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("COP");
    public static final Money ZERO = new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency != null ? currency : DEFAULT_CURRENCY;
    }

    public Money(BigDecimal amount) {
        this(amount, DEFAULT_CURRENCY);
    }

    public static Money zero() {
        return ZERO;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new IllegalArgumentException("El factor de multiplicación no puede ser nulo");
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public boolean isGreaterThan(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    private void checkSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("El monto a comparar no puede ser nulo");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("No se pueden realizar operaciones monetarias entre diferentes divisas: " 
                    + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return String.format("%s %.2f", currency.getCurrencyCode(), amount);
    }
}

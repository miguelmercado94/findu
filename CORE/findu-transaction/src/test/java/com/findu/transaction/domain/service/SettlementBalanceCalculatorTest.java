package com.findu.transaction.domain.service;

import com.findu.transaction.domain.service.SettlementBalanceCalculator.PendingAmounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementBalanceCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    @DisplayName("186300 con deuda 1300 => compensacion 1300 y neto 185000")
    void exampleFromRealData() {
        PendingAmounts r = SettlementBalanceCalculator.calculate(bd("186300"), bd("1300"));
        assertThat(r.gross()).isEqualByComparingTo("186300");
        assertThat(r.debtCompensation()).isEqualByComparingTo("1300");
        assertThat(r.net()).isEqualByComparingTo("185000");
        assertThat(r.remainingDebt()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Deuda mayor al bruto => neto 0 y deuda restante")
    void debtGreaterThanGross() {
        PendingAmounts r = SettlementBalanceCalculator.calculate(bd("1000"), bd("5000"));
        assertThat(r.debtCompensation()).isEqualByComparingTo("1000");
        assertThat(r.net()).isEqualByComparingTo("0");
        assertThat(r.remainingDebt()).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("Sin deuda => neto igual al bruto")
    void noDebt() {
        PendingAmounts r = SettlementBalanceCalculator.calculate(bd("50000"), BigDecimal.ZERO);
        assertThat(r.net()).isEqualByComparingTo("50000");
        assertThat(r.debtCompensation()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Bruto 0 o nulos => todo en cero, la deuda se conserva")
    void zeroGross() {
        PendingAmounts r = SettlementBalanceCalculator.calculate(null, bd("300"));
        assertThat(r.gross()).isEqualByComparingTo("0");
        assertThat(r.net()).isEqualByComparingTo("0");
        assertThat(r.remainingDebt()).isEqualByComparingTo("300");
    }
}
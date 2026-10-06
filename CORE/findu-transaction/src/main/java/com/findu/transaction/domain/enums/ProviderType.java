package com.findu.transaction.domain.enums;

public enum ProviderType {
    PERSONA_NATURAL(100000.0), // Límite de deuda conceptual COP $100.000
    CORPORATIVO(500000.0);      // Límite de deuda conceptual COP $500.000

    private final double debtLimitCop;

    ProviderType(double debtLimitCop) {
        this.debtLimitCop = debtLimitCop;
    }

    public double getDebtLimitCop() {
        return debtLimitCop;
    }
}

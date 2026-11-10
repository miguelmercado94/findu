package com.findu.transaction.domain.service;

import java.math.BigDecimal;

/**
 * Formula UNICA del saldo neto a liquidar de un proveedor.
 *
 * <pre>
 * compensation = min(gross, outstandingDebt)
 * net          = gross - compensation          (siempre &gt;= 0)
 * remainingDebt= outstandingDebt - compensation
 * </pre>
 *
 * Es la unica fuente de verdad para las pantallas de Finanzas (resumen semanal e historial)
 * y para la ejecucion de la liquidacion. Si la deuda es mayor o igual al bruto, el neto es 0
 * (no se transfiere nada) y la deuda restante queda a favor de la plataforma.
 */
public final class SettlementBalanceCalculator {

    private SettlementBalanceCalculator() {}

    public static PendingAmounts calculate(BigDecimal gross, BigDecimal outstandingDebt) {
        BigDecimal g = positiveOrZero(gross);
        BigDecimal debt = positiveOrZero(outstandingDebt);
        BigDecimal compensation = g.min(debt);
        return new PendingAmounts(g, compensation, g.subtract(compensation), debt.subtract(compensation));
    }

    private static BigDecimal positiveOrZero(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    public record PendingAmounts(BigDecimal gross, BigDecimal debtCompensation, BigDecimal net, BigDecimal remainingDebt) {}
}

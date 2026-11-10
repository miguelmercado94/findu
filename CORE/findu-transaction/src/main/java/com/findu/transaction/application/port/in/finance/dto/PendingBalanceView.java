package com.findu.transaction.application.port.in.finance.dto;

import com.findu.transaction.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Saldo pendiente de liquidar de un proveedor con desglose (bruto, compensacion de deuda, neto).
 * Lo produce UNICAMENTE {@code PendingBalanceService}; el resumen semanal y el historial lo comparten.
 */
@Getter
@Builder
public class PendingBalanceView {
    private final Money grossAmount;
    private final Money debtCompensationAmount;
    private final Money netAmount;
    /** Deuda de comisiones en efectivo que sigue pendiente despues de compensar. */
    private final Money remainingDebt;
    private final Money thisWeekGrossAmount;
    /** Bruto de semanas anteriores al lunes actual (arrastrado a la proxima liquidacion). */
    private final Money carriedOverGrossAmount;
    private final boolean carriedOver;
    private final int pendingServicesCount;
    private final LocalDate nextSettlementDate;
    private final LocalDate nextSettlementCutoff;
    /** Neto que se liquidara en {@link #nextSettlementDate} (lo ganado hasta su corte). */
    private final Money nextSettlementNet;
    private final LastSettlementView lastSettlement;

    @Getter
    @Builder
    public static class LastSettlementView {
        private final String settlementCode;
        private final String status;
        private final Money grossAmount;
        private final Money debtCompensationAmount;
        private final Money netAmount;
        private final Instant executedAt;
        private final String reference;
    }
}

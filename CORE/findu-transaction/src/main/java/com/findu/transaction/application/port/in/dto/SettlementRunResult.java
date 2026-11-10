package com.findu.transaction.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/** Resultado de ejecutar la liquidacion de un proveedor. */
@Getter
@Builder
public class SettlementRunResult {

    public enum Outcome { SETTLED, ALREADY_SETTLED, NOTHING_TO_SETTLE, NET_ZERO_NO_TRANSFER, FAILED }

    private final Long providerId;
    private final Outcome outcome;
    private final String settlementId;
    private final String settlementCode;
    private final BigDecimal grossAmount;
    private final BigDecimal debtCompensationAmount;
    private final BigDecimal netAmount;
    private final BigDecimal remainingDebt;
    private final boolean transferred;
    private final String payoutCode;
    private final String reference;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final Instant executedAt;
    private final int dailyBalancesSettled;
    private final String message;
}
